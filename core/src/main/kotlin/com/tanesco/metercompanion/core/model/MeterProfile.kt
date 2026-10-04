package com.tanesco.metercompanion.core.model

/**
 * A locally configured description of one electricity meter.
 *
 * This is *not* a live meter object: it holds the identity a customer gives the
 * meter they own. Anything that could only be known by talking to the meter
 * lives in [EnergySnapshot] instead, and is therefore nullable.
 */
data class MeterProfile(
    val id: String,
    /** The account or meter number printed on the meter. */
    val accountNumber: String,
    /** Customer-chosen label, e.g. "Kilimanjaro House". */
    val displayName: String,
    val category: MeterCategory,
    val phase: PhaseConfiguration,
    /** Optional servicing region or office. */
    val region: String?,
    /** Optional free-text note, never shown on shared surfaces. */
    val notes: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val isArchived: Boolean = false,
) {
    /**
     * The label to show in dense layouts where the account number is already
     * visible elsewhere on screen.
     */
    val subtitle: String get() = buildString {
        append(category.label)
        if (phase != PhaseConfiguration.Unknown) {
            append(" · ")
            append(phase.shortLabel)
        }
    }
}

/**
 * Meter billing category.
 *
 * [Unknown] is a legitimate user choice: customers are often unsure, and a
 * forced selection would be a worse experience than an explicit unknown.
 */
enum class MeterCategory(val label: String) {
    Prepaid("Prepaid"),
    Utility("Postpaid / utility"),
    Hybrid("Hybrid"),
    Unknown("Unspecified"),
}

/** Meter phase configuration. */
enum class PhaseConfiguration(val label: String, val shortLabel: String) {
    SinglePhase("Single phase", "1-ph"),
    ThreePhase("Three phase", "3-ph"),
    Unknown("Unspecified phase", "—"),
}

/**
 * One attempt to use a connection adapter, successful or not.
 *
 * Sessions are persisted so that Activity and Diagnostics can show what actually
 * happened without inventing a "last connected" claim.
 */
data class MeterSession(
    val id: String,
    val meterId: String?,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val outcome: SessionOutcome,
    /** Which adapter handled the attempt, e.g. `research-placeholder`. */
    val adapterId: String,
    /** Stable error code when the attempt did not succeed. */
    val failureCode: String? = null,
) {
    val durationMillis: Long?
        get() = endedAtMillis?.let { it - startedAtMillis }

    val isActive: Boolean get() = endedAtMillis == null
}

/**
 * Terminal state of a [MeterSession].
 *
 * Note the absence of a "connected" outcome: no adapter can currently reach a
 * meter, and the enum does not pretend otherwise.
 */
enum class SessionOutcome(val label: String) {
    Completed("Session completed"),
    Refused("Refused by adapter"),
    Unavailable("Capability unavailable"),
    Failed("Session failed"),
    Cancelled("Cancelled"),
}
