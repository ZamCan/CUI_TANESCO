package com.tanesco.metercompanion.core.token

import com.tanesco.metercompanion.core.history.EventRecorder
import com.tanesco.metercompanion.core.model.DomainError
import com.tanesco.metercompanion.core.model.DomainResult
import com.tanesco.metercompanion.core.model.EventCategory
import com.tanesco.metercompanion.core.model.EventNames
import com.tanesco.metercompanion.core.model.TokenHistory
import com.tanesco.metercompanion.core.model.TokenQuery
import com.tanesco.metercompanion.core.model.TokenRecord
import com.tanesco.metercompanion.core.model.TokenStatus
import com.tanesco.metercompanion.core.model.TokenTransferState
import com.tanesco.metercompanion.core.model.TokenValidationOutcome
import com.tanesco.metercompanion.core.model.TokenValidationReport
import com.tanesco.metercompanion.core.model.newRecord
import com.tanesco.metercompanion.core.repository.SettingsRepository
import com.tanesco.metercompanion.core.repository.TokenRepository
import com.tanesco.metercompanion.core.time.Clock
import com.tanesco.metercompanion.core.time.IdGenerator
import com.tanesco.metercompanion.core.validation.Fields
import kotlinx.coroutines.flow.Flow

/**
 * Live view of what typing a token would do, without storing anything.
 *
 * Returned by [TokenService.preview] so the Token Center can show normalised
 * text, grouped display text and per-rule verdicts while the user types.
 */
data class TokenPreview(
    val normalizedValue: String,
    val displayValue: String,
    val report: TokenValidationReport,
) {
    val isBlank: Boolean get() = normalizedValue.isEmpty()
}

/**
 * Application service for the Token Center.
 *
 * Responsibilities:
 *
 *  - normalise and group input
 *  - run the local structural checks
 *  - detect duplicates against the local history
 *  - persist the record and log what happened
 *
 * It deliberately stops there. Nothing in this class can report that a token was
 * accepted by a meter, because it has no way of knowing that; [TokenRecord]
 * carries a [TokenTransferState] that only a connection adapter may advance.
 */
class TokenService(
    private val tokenRepository: TokenRepository,
    private val settingsRepository: SettingsRepository,
    private val validationEngine: TokenValidationEngine,
    private val profiles: TokenFormatProfileRegistry,
    private val events: EventRecorder,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
) {

    fun observeHistory(query: TokenQuery): Flow<TokenHistory> = tokenRepository.observeHistory(query)

    suspend fun history(query: TokenQuery): TokenHistory = tokenRepository.getHistory(query)

    /** The format profile currently selected in settings. */
    suspend fun activeProfile(): TokenFormatProfile =
        profiles.resolve(settingsRepository.get().tokenFormatProfileId)

    /**
     * Normalises, validates and reports without persisting.
     *
     * Runs on every keystroke, so it performs no I/O.
     */
    suspend fun preview(rawInput: String): TokenPreview {
        val profile = activeProfile()
        val normalized = TokenNormalizer.normalize(rawInput)
        return TokenPreview(
            normalizedValue = normalized,
            displayValue = TokenNormalizer.formatForDisplay(normalized, profile.displayGroupSize),
            report = validationEngine.validateNormalized(normalized, profile),
        )
    }

    /**
     * Records a token.
     *
     * The returned record's [TokenRecord.status] is the honest local verdict:
     * [TokenStatus.Duplicate] when an identical canonical value already exists,
     * [TokenStatus.Rejected] when a blocking rule failed, and
     * [TokenStatus.StructurallyValid] otherwise.
     *
     * A duplicate or rejected token is still stored - the user asked for it to be
     * remembered - but it is flagged and an event is written, so the record can
     * never be mistaken for a token a meter accepted.
     */
    suspend fun record(rawInput: String, meterId: String?): DomainResult<TokenRecord> {
        val profile = activeProfile()
        val normalized = TokenNormalizer.normalize(rawInput)

        if (normalized.isEmpty()) {
            events.warning(
                category = EventCategory.Token,
                name = EventNames.TOKEN_VALIDATION_FAILED,
                summary = "Token entry was empty",
                meterId = meterId,
                userInitiated = true,
            )
            return DomainResult.Failure(
                DomainError.Validation(
                    code = "token.empty",
                    field = Fields.TOKEN,
                    detail = "Enter a token value.",
                ),
            )
        }

        val now = clock.nowMillis()
        val report = validationEngine.validateNormalized(normalized, profile)
        val previous = tokenRepository.findPreviousWithSameValue(
            normalizedValue = normalized,
            beforeMillis = now,
        )

        val status = when {
            previous != null -> TokenStatus.Duplicate
            report.outcome == TokenValidationOutcome.Failed -> TokenStatus.Rejected
            else -> TokenStatus.StructurallyValid
        }

        val record = TokenRecord.newRecord(
            id = idGenerator.newId(),
            normalizedValue = normalized,
            displayValue = TokenNormalizer.formatForDisplay(normalized, profile.displayGroupSize),
            status = status,
            meterId = meterId,
            clock = clock,
            validation = report,
        )

        val stored = tokenRepository.insert(record)
        if (stored is DomainResult.Failure) return stored

        when (status) {
            TokenStatus.Duplicate -> events.warning(
                category = EventCategory.Token,
                name = EventNames.TOKEN_DUPLICATE_DETECTED,
                summary = "Duplicate token recorded",
                detail = "Matches the token entered at ${previous?.enteredAtMillis ?: 0L}",
                meterId = meterId,
                userInitiated = true,
            )

            TokenStatus.Rejected -> events.warning(
                category = EventCategory.Token,
                name = EventNames.TOKEN_VALIDATION_FAILED,
                summary = "Token failed local checks",
                detail = report.failedChecks.joinToString("; ") { it.message },
                meterId = meterId,
                userInitiated = true,
            )

            else -> events.success(
                category = EventCategory.Token,
                name = EventNames.TOKEN_VALIDATION_PASSED,
                summary = "Token passed local checks",
                detail = "profile ${profile.id}",
                meterId = meterId,
                userInitiated = true,
            )
        }

        events.info(
            category = EventCategory.Token,
            name = EventNames.TOKEN_STORED,
            summary = "Token stored locally",
            meterId = meterId,
            detail = "status ${status.name}",
            userInitiated = true,
        )

        return DomainResult.Success(record)
    }

    /**
     * Advances a token's hand-off state.
     *
     * Only a connection adapter should call this, and only with a value it
     * actually observed. Today no code path reaches a state beyond
     * [TokenTransferState.NotAttempted].
     */
    suspend fun markTransferState(id: String, state: TokenTransferState): DomainResult<Unit> {
        val result = tokenRepository.setTransferState(id, state)
        if (result is DomainResult.Success) {
            events.info(
                category = EventCategory.Token,
                name = "token.transfer_state",
                summary = "Token hand-off state updated",
                detail = state.name,
            )
        }
        return result
    }
}
