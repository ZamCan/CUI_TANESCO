package com.tanesco.metercompanion.core.transport

import com.tanesco.metercompanion.core.ciu.CommunicationTechnology

/**
 * What a transport can actually do on this device.
 *
 * ## The distinction this type exists to protect
 *
 * "The phone has Bluetooth" and "the phone can talk to this meter" are different
 * claims, and conflating them is the characteristic error of CIU software. This
 * type keeps them apart:
 *
 *  - [physicalInterfacePresent] - the Android device exposes the radio or port at
 *    all. A hardware fact, discoverable at runtime.
 *  - [protocolCompatibility] - something speaking *our* protocol is known to be
 *    reachable over this interface. An engineering fact, established by
 *    experiment, never assumed.
 *
 * A transport is offered for a meter only when both hold. `Bluetooth present`
 * with `protocolCompatibility = Unestablished` yields no usable transport, and
 * the UI says why.
 */
enum class ProtocolCompatibility(val label: String) {
    /** No experiment has been performed. The honest default. */
    Unestablished("Not yet established"),

    /** Demonstrated to work with hardware in hand. */
    Confirmed("Confirmed with hardware"),

    /** Ruled out experimentally. */
    RuledOut("Ruled out by experiment"),

    /** Physically impossible with this interface. */
    Infeasible("Not physically possible"),
    ;

    val isUsable: Boolean get() = this == Confirmed
}

/**
 * Availability of a transport on the current device.
 *
 * Deliberately not a simple `isAvailable: Boolean`. The reasons a transport is
 * unusable differ in ways the user must be able to act on - a missing permission
 * is fixable, missing hardware is not, and a protocol that was never proven is a
 * different situation again.
 */
sealed interface TransportAvailability {

    /** Usable for the stated [technology]. */
    data class Available(val technology: CommunicationTechnology) : TransportAvailability

    /** The radio or port exists but something is blocking use. */
    data class Blocked(
        val technology: CommunicationTechnology,
        val reasonCode: String,
        val reason: String,
        /** True when the user can fix this by granting a permission. */
        val userCanResolve: Boolean,
    ) : TransportAvailability

    /** This device has no such interface at all. */
    data class HardwareAbsent(
        val technology: CommunicationTechnology,
        val reason: String,
    ) : TransportAvailability

    /** The interface exists but has never been shown to reach a real meter. */
    data class ProtocolNotEstablished(
        val technology: CommunicationTechnology,
        val reason: String,
    ) : TransportAvailability
}

/**
 * The runtime facts a transport needs from the host device.
 *
 * Only the fields an implementation actually uses are declared. Android's BLE
 * scanner and its USB host list are deliberately *not* modelled here: those are
 * transport implementations' concerns, and keeping them out of the port is what
 * lets a future desktop or iOS host implement the same interface.
 */
interface TransportContext {
    /** Stable identifier of the host, useful in session logs. */
    val hostId: String
}

/**
 * A physical link to a meter or CIU.
 *
 * A transport moves opaque bytes and knows nothing about meters, tokens or
 * protocols. Keeping it that dumb is deliberate: protocol behaviour belongs to
 * [com.tanesco.metercompanion.core.protocol.MeterProtocolAdapter], so a new
 * protocol can run over an existing transport without touching it, and a new
 * transport can carry an existing protocol without rewriting it.
 *
 * ### Implementors must
 *
 *  - report [getAvailability] truthfully, and never claim a protocol works
 *  - never fabricate a response: a timeout is [TransportResult.Failure] with
 *    [TransportFailure.Timeout], not an empty success
 *  - release resources in [close]
 */
interface MeterTransport {

    /** Stable identifier, e.g. `simulator`, `nfc`. */
    val id: String

    /** The technology this transport carries. */
    val technology: CommunicationTechnology

    /** Current availability, including *why* it is or is not usable. */
    fun getAvailability(): TransportAvailability

    /** Whether the protocol over this transport has been proven with hardware. */
    fun getProtocolCompatibility(): ProtocolCompatibility

    /** Opens the link. Idempotent. */
    suspend fun open(): TransportResult<Unit>

    /** Closes the link. Safe to call when already closed. */
    suspend fun close(): TransportResult<Unit>

    /** Sends [request] and returns what came back. */
    suspend fun exchange(
        request: TransportFrame,
        timeoutMillis: Long,
    ): TransportResult<TransportFrame>

    /** Releases resources. The transport is unusable afterwards. */
    fun release()
}

/**
 * One framed exchange.
 *
 * Bytes are opaque on purpose. A transport must not parse a payload, and this
 * type is what stops it from trying.
 */
data class TransportFrame(
    val bytes: ByteArray,
    val metadata: Map<String, String> = emptyMap(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransportFrame) return false
        return bytes.contentEquals(other.bytes) && metadata == other.metadata
    }

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + metadata.hashCode()
}

/** Outcome of a transport operation. */
sealed interface TransportResult<out T> {
    data class Success<out T>(val value: T) : TransportResult<T>
    data class Failure(val failure: TransportFailure) : TransportResult<Nothing>
}

/**
 * Why a transport operation failed.
 *
 * Every variant is an observable condition. None of them may be synthesised to
 * make a flow appear to work.
 */
sealed interface TransportFailure {

    val code: String

    /** Nothing answered within [timeoutMillis]. */
    data class Timeout(
        val timeoutMillis: Long,
    ) : TransportFailure {
        override val code: String get() = "transport.timeout"
    }

    /** The link was never opened, or has closed. */
    data class NotOpen(val reason: String) : TransportFailure {
        override val code: String get() = "transport.not_open"
    }

    /** The radio or port is unavailable. */
    data class Unavailable(val reason: String) : TransportFailure {
        override val code: String get() = "transport.unavailable"
    }

    /** A required runtime permission was refused. */
    data class PermissionDenied(val permission: String) : TransportFailure {
        override val code: String get() = "transport.permission_denied"
    }

    /** Bytes were sent but the reply was malformed. */
    data class MalformedFrame(val detail: String) : TransportFailure {
        override val code: String get() = "transport.malformed_frame"
    }

    /** The link dropped mid-exchange. */
    data class LinkLost(val detail: String) : TransportFailure {
        override val code: String get() = "transport.link_lost"
    }

    /** A defect in this transport implementation. */
    data class Internal(val detail: String, val cause: Throwable? = null) : TransportFailure {
        override val code: String get() = "transport.internal"
    }
}

/**
 * The transports known to the application.
 *
 * The registry is the single place a transport becomes selectable. Adding one
 * requires no change to any screen, which is what keeps the UI independent of
 * the physical layer.
 */
class TransportRegistry(private val transports: List<MeterTransport> = emptyList()) {

    fun all(): List<MeterTransport> = transports

    fun findById(id: String): MeterTransport? = transports.firstOrNull { it.id == id }

    /** Transports that are both present and protocol-proven. */
    fun usable(): List<MeterTransport> = transports.filter {
        it.getAvailability() is TransportAvailability.Available &&
            it.getProtocolCompatibility().isUsable
    }

    /**
     * Why a given transport cannot be used right now, for the connection screen.
     *
     * Returns `null` when the transport *is* usable, so a caller can render
     * either the capability or the reason without branching on emptiness.
     */
    fun explainUnusable(id: String): String? {
        val transport = findById(id) ?: return "No transport with id '$id' is registered."
        val compatibility = transport.getProtocolCompatibility()
        if (!compatibility.isUsable) {
            return "Communication over ${transport.technology.label} has not been " +
                "established with hardware (${compatibility.label})."
        }
        return when (val availability = transport.getAvailability()) {
            is TransportAvailability.Available -> null
            is TransportAvailability.Blocked -> availability.reason
            is TransportAvailability.HardwareAbsent -> availability.reason
            is TransportAvailability.ProtocolNotEstablished -> availability.reason
        }
    }
}
