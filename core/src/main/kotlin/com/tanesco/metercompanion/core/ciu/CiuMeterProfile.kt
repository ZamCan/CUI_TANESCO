package com.tanesco.metercompanion.core.ciu

/**
 * How a meter expects to be reached.
 *
 * Transport and protocol are separate axes on purpose. A phone having Bluetooth
 * does not mean it can talk to a 433 MHz FSK meter through a CIU; and a meter
 * speaking a given protocol family may be reachable over more than one physical
 * link. Collapsing the two produces the single most common false claim in this
 * domain, so they are modelled independently.
 */
enum class CommunicationTechnology(val label: String) {
    /** No physical link configured. */
    None("None"),

    /** RF link to a CIU, typically sub-GHz FSK/GFSK. */
    Radio("RF (sub-GHz)"),

    /** Bluetooth link, where the meter itself exposes BLE. */
    Bluetooth("Bluetooth"),

    /** Near-field coupling; short range and usually ticket-based. */
    Nfc("NFC"),

    /** Power-line carrier, where the meter talks over the mains network. */
    PowerLine("Power line (PLC)"),

    /** Wired serial/USB connection to a CIU. */
    Wired("Wired / serial"),

    /** In-process virtual device. See the simulator transport. */
    Simulated("Simulated"),
    ;

    val isPhysical: Boolean
        get() = this != None && this != Simulated
}

/**
 * Protocol family, kept coarse.
 *
 * Deliberately *not* a per-manufacturer enum. Individual manufacturers and
 * models are described by [CiuMeterProfile.manufacturer] and
 * [CiuMeterProfile.model] plus a [MeterProtocolAdapter], so adding a new meter
 * family never requires touching this enum.
 */
enum class ProtocolFamily(val label: String) {
    Unknown("Unknown"),
    Simulated("DIGITAL-CIU-SIM/1"),
    DlmsCosem("DLMS/COSEM"),
    ManufacturerProprietary("Manufacturer proprietary"),
}

/** Manufacturer identity, open-ended by design. */
data class Manufacturer(val id: String, val displayName: String)

/** Known manufacturer identifiers. Absence from this list means "unclassified". */
object Manufacturers {
    val EDMI = Manufacturer("edmi", "EDMI")
    val CLOU = Manufacturer("clou", "CLOU")
    val Wasion = Manufacturer("wasion", "Wasion")
    val LandisGyr = Manufacturer("landisgyr", "Landis+Gyr")
    val Actaris = Manufacturer("actaris", "Actaris")
    val Hexing = Manufacturer("hexing", "Hexing")
    val Unknown = Manufacturer("unknown", "Unclassified")
}

/**
 * A meter's RF channel configuration, where the technology uses one.
 *
 * Frequency is in hertz and is nullable throughout: no implementation in this
 * project may assert a frequency it has not read from a device or a documented
 * specification.
 */
data class RadioChannel(
    val channelIndex: Int? = null,
    val frequencyHz: Long? = null,
    val modulation: Modulation? = null,
)

enum class Modulation(val label: String) {
    Fsk("FSK"),
    Gfsk("GFSK"),
    Ask("ASK"),
    Psk("PSK"),
    OnOffKeying("OOK"),
    Unknown("Unknown"),
}

/** What a meter is willing and able to do. */
data class MeterCapabilities(
    val readIdentity: Boolean = false,
    val readBalance: Boolean = false,
    val readInstantaneous: Boolean = false,
    val readConsumptionHistory: Boolean = false,
    val readEvents: Boolean = false,
    val readStatus: Boolean = false,
    val submitToken: Boolean = false,
    val readDiagnostics: Boolean = false,
) {
    val any: Boolean
        get() = readIdentity || readBalance || readInstantaneous ||
            readConsumptionHistory || readEvents || readStatus ||
            submitToken || readDiagnostics

    companion object {
        /** Nothing supported. The honest default for an unclassified meter. */
        val None: MeterCapabilities = MeterCapabilities()
    }
}

/** Security posture, as *declared* by the meter or its documentation. */
data class SecurityProfile(
    /** Whether the meter expects authentication before answering. */
    val requiresAuthentication: Boolean = false,
    /** Whether association/pairing is part of the protocol. */
    val requiresAssociation: Boolean = false,
    /**
     * A human-readable description of the mechanism.
     *
     * Never holds key material. No implementation in this project extracts,
     * stores or derives meter credentials; see the security boundary in
     * docs/architecture/security-boundary.md.
     */
    val mechanism: String? = null,
)

/** Diagnostics captured from the device itself, never invented. */
data class MeterDiagnostics(
    val lastSuccessfulCommunicationMillis: Long? = null,
    val lastFailureCode: String? = null,
    val lastFailureDetail: String? = null,
    val signalStrength: Int? = null,
    val firmwareVersion: String? = null,
    val notes: String? = null,
)

/**
 * A meter's communication identity - the profile the Digital CIU reasons about.
 *
 * ## Why so many fields are nullable
 *
 * The observed evidence spans meters that pair over RF, meters that pair over
 * PLC, meters with a CIU serial and meters without any CIU at all; meters with a
 * configurable channel and meters with none; meters that report a serial and
 * meters that only carry an account number. A model that made any of these
 * mandatory would either reject real meters or invite invented values.
 *
 * Every field is therefore optional, and `null` always means *not known* - never
 * "default", never zero, and never a placeholder. This mirrors the
 * [com.tanesco.metercompanion.core.model.Measurement] rule already used for
 * readings, and for the same reason: the model must not be able to express a
 * fact nobody observed.
 *
 * ## Induced, not assumed
 *
 * Manufacturer and model are recorded per meter. No field is promoted to a
 * universal rule because one family happens to use it - see
 * docs/decisions/0008-model-specific-isolation.md.
 */
data class CiuMeterProfile(
    /** Stable local identifier. */
    val id: String,

    // --- Identity ----------------------------------------------------------
    /** The account or meter number a customer reads from the label. */
    val meterNumber: String? = null,
    /** The device's own serial, as reported by the device. */
    val meterSerial: String? = null,
    val manufacturer: Manufacturer = Manufacturers.Unknown,
    val model: String? = null,

    /** CIU serial where the system uses a separate customer interface unit. */
    val ciuSerial: String? = null,
    val ciuFirmwareVersion: String? = null,

    // --- Communication -----------------------------------------------------
    val technology: CommunicationTechnology = CommunicationTechnology.None,
    val protocolFamily: ProtocolFamily = ProtocolFamily.Unknown,
    val radioChannel: RadioChannel? = null,
    /** Communication address, where the protocol uses one. */
    val communicationAddress: String? = null,

    // --- State -------------------------------------------------------------
    val associationState: AssociationState = AssociationState.Unknown,
    val security: SecurityProfile = SecurityProfile(),
    val capabilities: MeterCapabilities = MeterCapabilities.None,
    val diagnostics: MeterDiagnostics = MeterDiagnostics(),

    // --- Provenance --------------------------------------------------------
    /**
     * How this profile's content was established.
     *
     * Recorded per profile because a simulator, a vendor datasheet and an
     * authorized real meter carry very different evidentiary weight, and the
     * product must never present one as another.
     */
    val source: MeterProfileSource = MeterProfileSource.Simulator,
) {

    /** A one-line label safe to show in a list. */
    val label: String
        get() = meterNumber ?: meterSerial ?: model ?: manufacturer.displayName

    /** True when nothing about this meter's identity has been established. */
    val isUnidentified: Boolean
        get() = meterNumber == null && meterSerial == null

    /** True when this profile describes a real device rather than a simulator. */
    val isSimulated: Boolean get() = source == MeterProfileSource.Simulator

    /**
     * Whether the application may issue requests.
     *
     * Requires *both* a usable transport and a state in which the meter will
     * answer. Either alone is insufficient, which is the point: a connected
     * transport to an unverified meter must not present as "ready".
     */
    val canExchangeData: Boolean
        get() = technology != CommunicationTechnology.None &&
            associationState.canExchangeData &&
            !isUnidentified
}

/**
 * The evidentiary weight of a profile.
 *
 * This is the mechanism behind the project's honesty requirement: the UI can
 * always show where a profile's contents came from, and a simulator can never be
 * presented as a production meter.
 */
enum class MeterProfileSource(val label: String) {
    /** A software device. No physical meter is involved. */
    Simulator("Simulator"),

    /** Legally acquired test equipment or an authorised test meter. */
    TestMeter("Test meter"),

    /** An authorised production meter, used with permission. */
    AuthorizedRealMeter("Authorised real meter"),

    /** Official utility or manufacturer integration. */
    UtilityProduction("Utility production"),
}
