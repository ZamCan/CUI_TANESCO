package com.tanesco.metercompanion.data.database

import com.tanesco.metercompanion.core.model.EventCategory
import com.tanesco.metercompanion.core.model.EventSeverity
import com.tanesco.metercompanion.core.model.MeterCategory
import com.tanesco.metercompanion.core.model.PhaseConfiguration
import com.tanesco.metercompanion.core.model.SessionOutcome
import com.tanesco.metercompanion.core.model.TokenCheckResult
import com.tanesco.metercompanion.core.model.TokenCheckSeverity
import com.tanesco.metercompanion.core.model.TokenStatus
import com.tanesco.metercompanion.core.model.TokenTransferState
import com.tanesco.metercompanion.core.model.TokenValidationOutcome
import com.tanesco.metercompanion.core.model.TokenValidationReport
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Conversions between domain enums and their stored representation.
 *
 * Enums are stored by *name*, not ordinal, so that reordering an enum can never
 * silently reinterpret existing rows.
 *
 * Every `fromStorage` function tolerates an unknown value and falls back to a
 * documented safe default. That keeps a database written by a newer build
 * readable by an older one instead of throwing on startup.
 */
object ValueCodecs {

    // --- meters ------------------------------------------------------------

    fun MeterCategory.toStorage(): String = name

    fun meterCategoryFromStorage(raw: String?): MeterCategory =
        raw?.let { value ->
            MeterCategory.entries.firstOrNull { it.name == value }
        } ?: MeterCategory.Unknown

    fun PhaseConfiguration.toStorage(): String = name

    fun phaseFromStorage(raw: String?): PhaseConfiguration =
        raw?.let { value ->
            PhaseConfiguration.entries.firstOrNull { it.name == value }
        } ?: PhaseConfiguration.Unknown

    // --- tokens ------------------------------------------------------------

    fun TokenStatus.toStorage(): String = name

    /** Unknown values fall back to [TokenStatus.Recorded], never to a passing state. */
    fun tokenStatusFromStorage(raw: String?): TokenStatus =
        raw?.let { value -> TokenStatus.entries.firstOrNull { it.name == value } }
            ?: TokenStatus.Recorded

    fun TokenTransferState.toStorage(): String = name

    /** Unknown values fall back to [TokenTransferState.NotAttempted]. */
    fun transferStateFromStorage(raw: String?): TokenTransferState =
        raw?.let { value -> TokenTransferState.entries.firstOrNull { it.name == value } }
            ?: TokenTransferState.NotAttempted

    // --- events ------------------------------------------------------------

    fun EventCategory.toStorage(): String = name

    /** Unknown values fall back to [EventCategory.System]. */
    fun eventCategoryFromStorage(raw: String?): EventCategory =
        raw?.let { value -> EventCategory.entries.firstOrNull { it.name == value } }
            ?: EventCategory.System

    fun EventSeverity.toStorage(): String = name

    fun eventSeverityFromStorage(raw: String?): EventSeverity =
        raw?.let { value -> EventSeverity.entries.firstOrNull { it.name == value } }
            ?: EventSeverity.Info

    // --- sessions ----------------------------------------------------------

    fun SessionOutcome.toStorage(): String = name

    fun sessionOutcomeFromStorage(raw: String?): SessionOutcome =
        raw?.let { value -> SessionOutcome.entries.firstOrNull { it.name == value } }
            ?: SessionOutcome.Failed

    // --- token validation report -------------------------------------------

    /**
     * Encodes a [TokenValidationReport] into a single TEXT column.
     *
     * A dedicated table with a join would be more relational, but a report is
     * always read and written whole and never queried by an individual check.
     * The encoding is therefore a compact, delimiter-separated form.
     *
     * Free-text fields (labels, messages) are Base64 encoded before being
     * joined so that a message containing the delimiters cannot corrupt the
     * record. The format is versioned by its own leading tag.
     */
    fun encodeValidation(report: TokenValidationReport?): String? {
        if (report == null) return null
        val builder = StringBuilder()
        builder.append(FORMAT_VERSION).append(FIELD_SEPARATOR)
        builder.append(report.outcome.name).append(FIELD_SEPARATOR)
        builder.append(encodeText(report.formatProfileId)).append(FIELD_SEPARATOR)
        builder.append(report.validatedAtMillis).append(FIELD_SEPARATOR)
        builder.append(report.checks.size)
        report.checks.forEach { check ->
            builder.append(FIELD_SEPARATOR)
            builder.append(encodeText(check.ruleId))
            builder.append(FIELD_SEPARATOR)
            builder.append(encodeText(check.label))
            builder.append(FIELD_SEPARATOR)
            builder.append(if (check.passed) '1' else '0')
            builder.append(FIELD_SEPARATOR)
            builder.append(check.severity.name)
            builder.append(FIELD_SEPARATOR)
            builder.append(encodeText(check.message))
        }
        return builder.toString()
    }

    /**
     * Decodes a report produced by [encodeValidation].
     *
     * Returns `null` for `NULL`, an empty value, or an unrecognised format
     * version. A corrupt report must never crash the token list; losing the
     * detailed checks is preferable to losing the token record itself.
     */
    fun decodeValidation(raw: String?): TokenValidationReport? {
        if (raw.isNullOrEmpty()) return null
        val parts = raw.split(FIELD_SEPARATOR)
        if (parts.size < MIN_ENCODED_FIELDS) return null
        if (parts[0] != FORMAT_VERSION) return null

        val outcome = TokenValidationOutcome.entries.firstOrNull { it.name == parts[1] } ?: return null
        val profileId = decodeText(parts[2]) ?: return null
        val validatedAt = parts[3].toLongOrNull() ?: return null
        val checkCount = parts[4].toIntOrNull()?.takeIf { it >= 0 } ?: return null

        val expectedSize = MIN_ENCODED_FIELDS + (checkCount * FIELDS_PER_CHECK)
        if (parts.size != expectedSize) return null

        val checks = ArrayList<TokenCheckResult>(checkCount)
        for (index in 0 until checkCount) {
            val offset = MIN_ENCODED_FIELDS + (index * FIELDS_PER_CHECK)
            val ruleId = decodeText(parts[offset])
            val label = decodeText(parts[offset + 1])
            val passed = parts[offset + 2] == "1"
            val severity = TokenCheckSeverity.entries.firstOrNull { it.name == parts[offset + 3] }
            val message = decodeText(parts[offset + 4])
            if (ruleId == null || label == null || severity == null || message == null) return null
            checks.add(
                TokenCheckResult(
                    ruleId = ruleId,
                    label = label,
                    passed = passed,
                    severity = severity,
                    message = message,
                ),
            )
        }

        return TokenValidationReport(
            outcome = outcome,
            checks = checks,
            formatProfileId = profileId,
            validatedAtMillis = validatedAt,
        )
    }

    private const val FORMAT_VERSION = "v1"
    private const val FIELD_SEPARATOR = '\u001F'
    private const val MIN_ENCODED_FIELDS = 5
    private const val FIELDS_PER_CHECK = 5

    private fun encodeText(value: String): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String? = try {
        String(
            Base64.getUrlDecoder().decode(value),
            StandardCharsets.UTF_8,
        )
    } catch (illegal: IllegalArgumentException) {
        null
    }
}
