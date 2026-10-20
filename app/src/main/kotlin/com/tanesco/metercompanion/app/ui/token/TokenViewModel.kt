package com.tanesco.metercompanion.app.ui.token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanesco.metercompanion.core.model.DomainError
import com.tanesco.metercompanion.core.model.MeterProfile
import com.tanesco.metercompanion.core.model.TokenHistory
import com.tanesco.metercompanion.core.model.TokenQuery
import com.tanesco.metercompanion.core.model.TokenRecord
import com.tanesco.metercompanion.core.meter.MeterProfileService
import com.tanesco.metercompanion.core.token.MeterKeypadInput
import com.tanesco.metercompanion.core.token.TokenNormalizer
import com.tanesco.metercompanion.core.token.TokenPreview
import com.tanesco.metercompanion.core.token.TokenService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the Token Center renders. */
data class TokenUiState(
    /** The meter's digit buffer, and the single source of truth for the entry. */
    val keypad: MeterKeypadInput = MeterKeypadInput(maxDigits = DEFAULT_CAPACITY),
    val displayPreview: String = "",
    val isBlank: Boolean = true,
    /** Number of local structural checks currently failing. */
    val failedCheckCount: Int = 0,
    val history: TokenHistory = TokenHistory.empty(),
    val meters: List<MeterProfile> = emptyList(),
    val selectedMeterId: String? = null,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
) {
    /** The typed entry, for the accessible keyboard alternative. */
    val typedValue: String get() = keypad.digits
}

/** One-shot outcome of a submission, shown once and then cleared. */
sealed interface TokenSubmissionResult {
    data object Idle : TokenSubmissionResult
    data class Stored(val record: TokenRecord) : TokenSubmissionResult
    data class Rejected(val message: String) : TokenSubmissionResult
}

/** Intermediate groupings, so no single `combine` needs more than four flows. */
private data class EntrySnapshot(
    val keypad: MeterKeypadInput = MeterKeypadInput(maxDigits = DEFAULT_CAPACITY),
    val preview: TokenPreview? = null,
    val submitting: Boolean = false,
)

private data class HistorySnapshot(
    val history: TokenHistory = TokenHistory.empty(),
    val meters: List<MeterProfile> = emptyList(),
    val selectedMeterId: String? = null,
)

/**
 * Drives the Token Center.
 *
 * Entry is modelled on the physical meter's keypad: [MeterKeypadInput] holds the
 * digit buffer, its capacity and the display page, and every key press goes
 * through it. The on-screen keyboard field writes into the same buffer, so the
 * two input methods cannot disagree.
 *
 * The local checks applied to the entry are explicitly *structural* - length,
 * character set, obvious typos - and the screen says so. Only a meter can decide
 * whether a token is valid, and no meter is reachable in this build.
 */
@OptIn(FlowPreview::class)
class TokenViewModel(
    private val tokens: TokenService,
    private val meterProfiles: MeterProfileService,
) : ViewModel() {

    private val keypad = MutableStateFlow(MeterKeypadInput(maxDigits = DEFAULT_CAPACITY))
    private val submitting = MutableStateFlow(false)
    private val result = MutableStateFlow<TokenSubmissionResult>(TokenSubmissionResult.Idle)
    private val meterFilter = MutableStateFlow<String?>(null)

    val submission: StateFlow<TokenSubmissionResult> = result.asStateFlow()

    /** Debounced so the rule pipeline runs when the user pauses, not per keystroke. */
    private val previewFlow: Flow<TokenPreview> = keypad
        .map { it.digits }
        .debounce(PREVIEW_DEBOUNCE_MILLIS)
        .distinctUntilChanged()
        .map { digits -> tokens.preview(digits) }

    private val entrySnapshot: Flow<EntrySnapshot> =
        combine(keypad, previewFlow, submitting) { buffer, preview, busy ->
            EntrySnapshot(keypad = buffer, preview = preview, submitting = busy)
        }

    private val historySnapshot: Flow<HistorySnapshot> = combine(
        meterFilter,
        tokens.observeHistory(TokenQuery(limit = HISTORY_LIMIT)),
        meterProfiles.observeAll(),
        meterProfiles.observeDefaultMeter(),
    ) { filter, history, allMeters, defaultMeter ->
        HistorySnapshot(
            history = history,
            meters = allMeters.filterNot { it.isArchived },
            selectedMeterId = filter ?: defaultMeter?.id,
        )
    }

    val state: StateFlow<TokenUiState> =
        combine(entrySnapshot, historySnapshot) { entry, history ->
            TokenUiState(
                keypad = entry.keypad,
                displayPreview = entry.preview?.displayValue.orEmpty(),
                isBlank = entry.keypad.isEmpty,
                failedCheckCount = entry.preview?.report?.failedChecks?.size ?: 0,
                history = history.history,
                meters = history.meters,
                selectedMeterId = history.selectedMeterId,
                isLoading = false,
                isSubmitting = entry.submitting,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = TokenUiState(),
        )

    // --- Keypad input -------------------------------------------------------

    fun pressDigit(digit: Char) {
        keypad.value = keypad.value.pressDigit(digit)
    }

    /** One step back. The meter's keypad has no dedicated backspace key. */
    fun pressBackspace() {
        keypad.value = keypad.value.pressBackspace()
    }

    /** CANCEL: discards the whole entry, as the hardware does. */
    fun pressCancel() {
        keypad.value = keypad.value.pressCancel()
    }

    fun pressPageNext() {
        keypad.value = keypad.value.pressPageNext()
    }

    fun pressPagePrevious() {
        keypad.value = keypad.value.pressPagePrevious()
    }

    /**
     * Accepts typed or pasted input.
     *
     * Normalised first, so a pasted token with spaces or dashes fills the buffer
     * correctly. Characters outside the digit set are dropped rather than
     * rejected, matching the keypad's own tolerance.
     */
    fun onTypedValueChanged(raw: String) {
        val normalized = TokenNormalizer.normalize(raw)
        val digitsOnly = normalized.filter { it.isDigit() }
        keypad.value = MeterKeypadInput(
            maxDigits = keypad.value.maxDigits,
            digitsPerPage = keypad.value.digitsPerPage,
            digits = digitsOnly,
        )
    }

    /** Restricts history to one meter, or shows every meter when `null`. */
    fun setMeterFilter(meterId: String?) {
        meterFilter.value = meterId
    }

    fun consumeSubmissionResult() {
        result.value = TokenSubmissionResult.Idle
    }

    /** ENTER: stores the entry locally. Nothing is sent to a meter. */
    fun submit() {
        if (submitting.value) return
        if (state.value.isBlank) {
            result.value = TokenSubmissionResult.Rejected("Enter a token value.")
            return
        }

        val digits = keypad.value.digits
        val meterId = meterFilter.value ?: state.value.selectedMeterId

        viewModelScope.launch {
            submitting.value = true
            try {
                val outcome = tokens.record(digits, meterId)
                result.value = outcome.fold(
                    onSuccess = { TokenSubmissionResult.Stored(it) },
                    onFailure = { error ->
                        TokenSubmissionResult.Rejected(error.tokenRejectionMessage())
                    },
                )
                // The buffer is cleared only after a confirmed local write, so a
                // failed write never loses what the user typed.
                if (outcome.isSuccess) keypad.value = keypad.value.pressCancel()
            } finally {
                submitting.value = false
            }
        }
    }

    private companion object {
        const val HISTORY_LIMIT = 50
        const val PREVIEW_DEBOUNCE_MILLIS = 120L
    }
}

/**
 * Fallback capacity used before the format profile has been read.
 *
 * Matches the shipped structural profile, so the keypad is never briefly
 * undersized on first composition.
 */
private const val DEFAULT_CAPACITY = 20

/** Explains why a token was refused, in the user's terms. */
internal fun DomainError.tokenRejectionMessage(): String = when (this) {
    is DomainError.Validation -> detail
    is DomainError.Storage -> "The token could not be saved. Local storage may be unavailable."
    is DomainError.Unexpected -> "The token could not be saved."
    else -> technicalDetail
}
