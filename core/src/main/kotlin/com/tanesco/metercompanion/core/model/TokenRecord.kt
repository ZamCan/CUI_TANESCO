package com.tanesco.metercompanion.core.model

import com.tanesco.metercompanion.core.time.Clock

/**
 * A utility token that the user has entered into the application and that has
 * been stored locally.
 *
 * The wording throughout this file is deliberate. A token is only ever
 * *recorded* and *structurally checked* locally. Whether a meter accepts it is
 * a question for a connection adapter, and until one exists the application has
 * no basis for such a claim.
 */
data class TokenRecord(
    val id: String,
    /**
     * Canonical form: uppercase with all separators removed. This is the value
     * used for duplicate detection and comparison.
     */
    val normalizedValue: String,
    /** Grouped form for reading, e.g. `1234 5678 9012 3456`. */
    val displayValue: String,
    val status: TokenStatus,
    /** The meter this token was entered against, when one is selected. */
    val meterId: String?,
    val enteredAtMillis: Long,
    val validation: TokenValidationReport?,
    /**
     * Whether a connection adapter has attempted to hand this token to a meter.
     *
     * [TokenTransferState.NotAttempted] is the only reachable value today.
     */
    val transferState: TokenTransferState = TokenTransferState.NotAttempted,
    val note: String? = null,
) {
    /** Local structural verdict, phrased so it cannot be read as meter acceptance. */
    val localCheckLabel: String get() = status.label

    companion object {
        /**
         * Factory that stamps a record with the current time.
         *
         * Centralising construction means no caller can forget a timestamp,
         * which would otherwise silently corrupt "recent activity" ordering.
         */
        fun create(
            id: String,
            normalizedValue: String,
            displayValue: String,
            status: TokenStatus,
            meterId: String?,
            clock: Clock,
            validation: TokenValidationReport? = null,
        ): TokenRecord = TokenRecord(
            id = id,
            normalizedValue = normalizedValue,
            displayValue = displayValue,
            status = status,
            meterId = meterId,
            enteredAtMillis = clock.nowMillis(),
            validation = validation,
        )
    }
}

/** Outcome of the application's own, local, structural checks. */
enum class TokenStatus(val label: String) {
    /** Stored locally, not yet checked. */
    Recorded("Recorded locally"),

    /** Passed every local structural check. Says nothing about meter acceptance. */
    StructurallyValid("Passed local checks"),

    /** Failed at least one local structural check. */
    Rejected("Failed local checks"),

    /** Identical to a token already recorded earlier. */
    Duplicate("Duplicate of an earlier token"),
}

/**
 * Progress of a token hand-off to a physical meter.
 *
 * No state beyond [NotAttempted] can currently be produced, because no adapter
 * is able to reach a meter. The states exist so that a future, legitimate
 * adapter can report truthfully without changing the persistence model.
 */
enum class TokenTransferState(val label: String) {
    NotAttempted("Not sent to a meter"),

    /** Queued for an adapter that is not yet available. */
    AwaitingAdapter("Awaiting connection support"),

    /** An adapter has taken responsibility for the attempt. */
    AcceptedByAdapter("Accepted by connection adapter"),

    /** An adapter reported the meter confirmed the token. */
    ConfirmedByMeter("Confirmed by meter"),

    /** An adapter reported the attempt failed. */
    FailedByAdapter("Attempt failed"),
}

/** Result of running the local validation pipeline. */
data class TokenValidationReport(
    val outcome: TokenValidationOutcome,
    val checks: List<TokenCheckResult>,
    /** Identifies the [TokenFormatProfile] the checks were taken from. */
    val formatProfileId: String,
    val validatedAtMillis: Long,
) {
    val failedChecks: List<TokenCheckResult> get() = checks.filterNot { it.passed }
}

enum class TokenValidationOutcome(val label: String) {
    Passed("All local checks passed"),
    Failed("One or more local checks failed"),
}

/** One rule's verdict. [message] is user-facing copy for this specific rule. */
data class TokenCheckResult(
    val ruleId: String,
    val label: String,
    val passed: Boolean,
    val severity: TokenCheckSeverity,
    val message: String,
)

/** Whether a failed rule is a hard rejection or an advisory warning. */
enum class TokenCheckSeverity { Error, Warning }

/** Filter for the token history list. */
data class TokenQuery(
    val meterId: String? = null,
    val status: TokenStatus? = null,
    val limit: Int = 100,
    val sinceMillis: Long? = null,
) {
    init {
        require(limit > 0) { "limit must be positive" }
    }
}

/**
 * A searchable, reorderable set of locally entered tokens for the Token Center.
 */
data class TokenHistory(
    val records: List<TokenRecord>,
    val totalCount: Int,
) {
    val isEmpty: Boolean get() = records.isEmpty()

    companion object {
        fun empty(): TokenHistory = TokenHistory(emptyList(), 0)
    }
}
