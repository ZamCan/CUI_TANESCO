package com.tanesco.metercompanion.core.experiment

/**
 * How much weight an experimental result carries.
 *
 * This enum is the scientific honesty mechanism of the project. Every recorded
 * observation must state its confidence, and no observation may be promoted past
 * its evidence. In particular:
 *
 *  - a **single** observed behaviour is at most [StronglySupported]
 *  - a behaviour seen on one model family is never a universal rule
 *  - [Hypothesis] and [Unknown] exist so that gaps are recorded rather than
 *    quietly omitted, because an omitted gap becomes an invented assumption later
 */
enum class Confidence(val label: String, val rank: Int) {
    /** No evidence. Recorded so the gap is visible. */
    Unknown("Unknown", 0),

    /** A plausible expectation, not yet tested. */
    Hypothesis("Hypothesis", 1),

    /** Some evidence, but incomplete or on one configuration only. */
    PartiallyConfirmed("Partially confirmed", 2),

    /** Repeatedly observed, but not across independent systems. */
    StronglySupported("Strongly supported", 3),

    /** Observed on multiple independent systems, or documented by the vendor. */
    Confirmed("Confirmed", 4),
    ;

    val isReliable: Boolean get() = this == Confirmed || this == StronglySupported
}

/**
 * What kind of hardware produced an observation.
 *
 * Recorded per experiment because evidence from a simulator proves something
 * different from evidence obtained with vendor development equipment, and
 * different again from an authorised production meter.
 */
enum class TestRig(val label: String) {
    /** Software only. Proves architecture, never physics. */
    Simulator("Simulator"),

    /** Vendor development equipment or a bench setup. */
    DevelopmentEquipment("Development equipment"),

    /** Legally acquired test meter. */
    TestMeter("Test meter"),

    /** Authorised production meter, with permission. */
    AuthorizedRealMeter("Authorised real meter"),
}

/**
 * One recorded experiment.
 *
 * Every field the specification asks for is present and required: an experiment
 * with no [confidence] or no [unknowns] recorded is not accepted. That is the
 * point - it forces the negative findings and the open questions to be written
 * down at the time of the experiment, when they are still known, rather than
 * reconstructed later when they have been forgotten.
 */
data class ExperimentRecord(
    val experimentId: String,
    /** Epoch millis; converted for display rather than stored as a string. */
    val performedAtMillis: Long,
    val equipment: String,
    val manufacturer: String,
    val model: String?,
    val rig: TestRig,
    val transport: String,
    /** What the documentation says should happen. */
    val expectedBehavior: String,
    /** What the specification or prior experiments suggested. */
    val hypothesis: String?,
    /** What was actually observed. */
    val observedBehavior: String,
    val result: ExperimentOutcome,
    /** How the result was captured: a capture file, a log excerpt, a video. */
    val evidence: String?,
    val confidence: Confidence,
    /** Questions this experiment raised. Never empty in practice. */
    val unknowns: List<String>,
    /** The next experiment this result suggests. */
    val nextExperiment: String?,
) {
    init {
        require(experimentId.isNotBlank()) { "experimentId must not be blank" }
        require(observedBehavior.isNotBlank()) { "observedBehavior must not be blank" }
    }

    /** True when the evidence is strong enough to design against. */
    val isDesignGrade: Boolean get() = confidence.isReliable

    /**
     * True when this record must not be presented as a production behaviour.
     *
     * Simulator evidence is always excluded, which is what prevents a simulated
     * result from being quoted as though it described a real meter.
     */
    val isProductionEvidence: Boolean
        get() = rig != TestRig.Simulator && confidence >= Confidence.StronglySupported
}

enum class ExperimentOutcome(val label: String) {
    /** Observed behaviour matched the expectation. */
    AsExpected("As expected"),

    /** Observed behaviour differed from the expectation. */
    Unexpected("Unexpected result"),

    /** Nothing could be observed; the attempt did not complete. */
    Inconclusive("Inconclusive"),

    /** The attempt was not possible with the equipment available. */
    NotAttempted("Not attempted"),
}

/**
 * A behaviour observed in at least one real CIU system.
 *
 * ## How this type prevents the central mistake
 *
 * The specification this project is induced from warns against promoting one
 * manufacturer's behaviour to a universal rule. That is enforced structurally:
 *
 *  - [observedOn] lists every system the behaviour was actually seen on
 *  - [universal] may only be true once [observedOn] holds more than one entry
 *  - [evidenceConfidence] is capped at [Confidence.StronglySupported] until a
 *    second independent system confirms it
 *
 * A behaviour seen on exactly one meter therefore *cannot* be declared
 * universal, no matter how convincing the single observation was.
 */
data class ObservedBehavior(
    val id: String,
    val description: String,
    val protocolFamilies: Set<String>,
    /** Systems this was actually observed on. Empty means hearsay. */
    val observedOn: List<ObservationSite>,
    val evidenceConfidence: Confidence,
    /**
     * True only when the behaviour has been confirmed across independent
     * systems. Asserted in [init] rather than trusted.
     */
    val universal: Boolean = false,
    /** What remains unknown about this behaviour. */
    val unknowns: List<String> = emptyList(),
) {
    init {
        require(description.isNotBlank()) { "description must not be blank" }
        if (universal) {
            require(observedOn.size >= MIN_SITES_FOR_UNIVERSAL) {
                "A behaviour cannot be universal until it has been observed on at least " +
                    "$MIN_SITES_FOR_UNIVERSAL independent systems; it was observed on " +
                    "${observedOn.size}."
            }
            require(evidenceConfidence == Confidence.Confirmed) {
                "A universal behaviour requires Confirmed evidence, but this is " +
                    "${evidenceConfidence.label}."
            }
        }
    }

    /** Distinct manufacturer families this behaviour was seen on. */
    val distinctManufacturers: Int
        get() = observedOn.map { it.manufacturer }.distinct().size

    /**
     * The effective confidence, capped by how many systems were involved.
     *
     * A single-site observation is demoted from `Confirmed` to
     * `StronglySupported`, because confirmation requires independence.
     */
    val effectiveConfidence: Confidence
        get() = if (distinctManufacturers < MIN_SITES_FOR_UNIVERSAL) {
            minOf(evidenceConfidence, Confidence.StronglySupported)
        } else {
            evidenceConfidence
        }

    companion object {
        /** Independent systems required before "universal" is permitted. */
        const val MIN_SITES_FOR_UNIVERSAL = 2
    }
}

/** One site at which a behaviour was observed. */
data class ObservationSite(
    val manufacturer: String,
    val model: String,
    val rig: TestRig,
    val experimentIds: List<String>,
) {
    init {
        require(experimentIds.isNotEmpty()) {
            "An observation must cite at least one experiment."
        }
    }
}
