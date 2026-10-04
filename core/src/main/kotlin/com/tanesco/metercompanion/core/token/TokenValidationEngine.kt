package com.tanesco.metercompanion.core.token

import com.tanesco.metercompanion.core.model.TokenCheckResult
import com.tanesco.metercompanion.core.model.TokenValidationOutcome
import com.tanesco.metercompanion.core.model.TokenValidationReport
import com.tanesco.metercompanion.core.time.Clock

/**
 * Runs the structural rules of a [TokenFormatProfile] and produces a report.
 *
 * The engine is pure: it reads no clock of its own beyond the [Clock] it is
 * given, performs no I/O, and knows nothing about meters. Its single job is to
 * answer "does this value satisfy the documented structural rules?".
 */
class TokenValidationEngine(
    private val profiles: TokenFormatProfileRegistry,
    private val clock: Clock,
) {

    /**
     * Validates [rawInput] exactly as the user typed it.
     *
     * Normalisation happens here so that no caller can accidentally validate a
     * value that still contains separators.
     */
    fun validate(rawInput: String, formatProfileId: String): TokenValidationReport {
        val profile = profiles.resolve(formatProfileId)
        return validateNormalized(TokenNormalizer.normalize(rawInput), profile)
    }

    /** Validates an already canonical value against [profile]. */
    fun validateNormalized(normalized: String, profile: TokenFormatProfile): TokenValidationReport {
        val checks = profile.buildRules().map { rule ->
            val verdict = rule.check(normalized)
            TokenCheckResult(
                ruleId = rule.id,
                label = rule.label,
                passed = verdict.passed,
                severity = verdict.severity,
                message = verdict.message,
            )
        }

        // A report only fails when an *Error* severity rule failed; a Warning
        // is surfaced to the user without rejecting the value.
        val outcome = if (checks.any { !it.passed && it.severity.isBlocking() }) {
            TokenValidationOutcome.Failed
        } else {
            TokenValidationOutcome.Passed
        }

        return TokenValidationReport(
            outcome = outcome,
            checks = checks,
            formatProfileId = profile.id,
            validatedAtMillis = clock.nowMillis(),
        )
    }

    /** Formats [rawInput] for display using [formatProfileId]'s grouping. */
    fun formatForDisplay(rawInput: String, formatProfileId: String): String {
        val profile = profiles.resolve(formatProfileId)
        return TokenNormalizer.formatForDisplay(
            normalized = TokenNormalizer.normalize(rawInput),
            groupSize = profile.displayGroupSize,
        )
    }
}

private fun com.tanesco.metercompanion.core.model.TokenCheckSeverity.isBlocking(): Boolean =
    this == com.tanesco.metercompanion.core.model.TokenCheckSeverity.Error
