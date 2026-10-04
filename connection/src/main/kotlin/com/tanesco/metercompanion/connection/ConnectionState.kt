package com.tanesco.metercompanion.connection

import com.tanesco.metercompanion.core.model.EnergySnapshot

/**
 * The state of a [MeterConnection].
 *
 * Modelled as a sealed hierarchy rather than a flat enum because each state
 * carries information the UI must present honestly: a state that means "we
 * chose not to build this yet" has to be distinguishable from one that means
 * "the hardware is not present", and neither may look like success.
 *
 * See docs/connection/CONNECTION-BOUNDARY.md for the full contract.
 */
sealed interface ConnectionState {

    /** Short, stable label. Safe for persistence and for accessibility text. */
    val label: String

    /** No connection has been requested, or the last one was closed. */
    data object Disconnected : ConnectionState {
        override val label: String get() = "Not connected"
    }

    /** A connection attempt is in progress. */
    data class Connecting(val adapterId: String) : ConnectionState {
        override val label: String get() = "Connecting"
    }

    /**
     * A live session with a meter.
     *
     * No implementation can currently produce this state. It exists because a
     * future, authorised adapter must be able to report it without the domain
     * model changing.
     */
    data class Connected(
        val adapterId: String,
        val sessionId: String,
        val sinceMillis: Long,
    ) : ConnectionState {
        override val label: String get() = "Connected"
    }

    /**
     * The adapter exists but cannot be used right now: no hardware, permission
     * not granted, or the required transport is missing on this device.
     */
    data class Unavailable(
        val code: String,
        val reason: String,
    ) : ConnectionState {
        override val label: String get() = "Connection unavailable"
    }

    /**
     * The capability is deliberately not implemented because it requires
     * technical research that has not been performed.
     *
     * This is the honest state for every meter operation today, and it must
     * never be rendered as a failure or as a success.
     */
    data class Research(
        val scope: String,
        val note: String,
    ) : ConnectionState {
        override val label: String get() = "Under research"
    }

    /** Something went wrong that is not the user's doing. */
    data class Failed(
        val code: String,
        val reason: String,
        val recoverable: Boolean,
    ) : ConnectionState {
        override val label: String get() = "Connection error"
    }
}

/**
 * What a connection implementation is able to do.
 *
 * Every field is a *capability declaration*, not an implementation detail, so
 * the UI can grey out or hide an action instead of offering something that will
 * fail.
 */
data class ConnectionCapabilities(
    val id: String,
    val displayName: String,
    /** Whether this adapter is ready for use, still under study, or unusable. */
    val availability: AdapterAvailability,
    val canReadMeterIdentity: Boolean,
    val canReadElectricalMeasurements: Boolean,
    val canReadTokenCredit: Boolean,
    val canSubmitToken: Boolean,
    val requiresPhysicalProximity: Boolean,
    /** Honest, human readable caveats. Shown verbatim on the Meter screen. */
    val notes: List<String> = emptyList(),
) {
    val hasAnyMeterCapability: Boolean
        get() = canReadMeterIdentity || canReadElectricalMeasurements ||
            canReadTokenCredit || canSubmitToken
}

/** Readiness of an adapter implementation. */
enum class AdapterAvailability(val label: String) {
    /** Fully implemented and usable. */
    Ready("Ready"),

    /** Deliberately not implemented; requires further technical research. */
    Research("Research"),

    /** Cannot run in this build or on this device. */
    Unavailable("Unavailable"),
}

/**
 * Snapshot of a meter's own reported data.
 *
 * [energy] is [EnergySnapshot.Empty] whenever nothing has actually been read.
 * There is no way to construct an instance that claims measurements without
 * declaring their provenance.
 */
data class MeterSnapshot(
    val meterSerial: String? = null,
    val meterType: String? = null,
    val energy: EnergySnapshot = EnergySnapshot.Empty,
    val capturedAtMillis: Long? = null,
) {
    val hasReportedData: Boolean
        get() = meterSerial != null || meterType != null || !energy.isEmpty
}

/** A live connection session handed out by an adapter. */
data class ConnectionSession(
    val id: String,
    val adapterId: String,
    val meterId: String? = null,
    val startedAtMillis: Long,
)

/**
 * What the caller wants to connect to.
 *
 * [meterId] refers to a locally configured profile, never to a discovered
 * physical device.
 */
data class ConnectionRequest(
    val meterId: String?,
    /** Set when the user explicitly confirmed they are at the meter. */
    val userConfirmedProximity: Boolean = false,
)
