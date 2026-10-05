package com.tanesco.metercompanion.app.ui.token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanesco.metercompanion.core.model.DomainError
import com.tanesco.metercompanion.core.model.MeterProfile
import com.tanesco.metercompanion.core.model.TokenHistory
import com.tanesco.metercompanion.core.model.TokenQuery
import com.tanesco.metercompanion.core.model.TokenRecord
import com.tanesco.metercompanion.core.meter.MeterProfileService
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
    val input: String = "",
    val displayPreview: String = "",
    val isBlank: Boolean = true,
    /** Number of local structural checks currently failing. */
    val failedCheckCount: Int = 0,
    val history: TokenHistory = TokenHistory.empty(),
    val meters: List<MeterProfile> = emptyList(),
    val selectedMeterId: String? = null,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
)

/** One-shot outcome of a submission, shown once and then cleared. */
sealed interface TokenSubmissionResult {
    data object Idle : TokenSubmissionResult
    data class Stored(val record: TokenRecord) : TokenSubmissionResult
    data class Rejected(val message: String) : TokenSubmissionResult
}

/** Intermediate grouping, so no single `combine` needs more than four flows. */
private data class InputSnapshot(
    val raw: String = "",
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
 * The field is checked locally as the user types. That check is explicitly
 * *structural* - length, character set, obvious typos - and the screen says so,
 * because a passing check is easy to misread as "this token is valid". Only a
 * meter can decide that, and no meter is reachable in this build.
 */
@OptIn(FlowPreview::class)
class TokenViewModel(
    private val tokens: TokenService,
    private val meterProfiles: MeterProfileService,
) : ViewModel() {

    private val input = MutableStateFlow("")
    private val submitting = MutableStateFlow(false)
    private val result = MutableStateFlow<TokenSubmissionResult>(TokenSubmissionResult.Idle)
    private val meterFilter = MutableStateFlow<String?>(null)

    val submission: StateFlow<TokenSubmissionResult> = result.asStateFlow()

    /**
     * Debounced so that the local rule pipeline runs once the user pauses,
     * rather than on every keystroke.
     */
    private val previewFlow: Flow<TokenPreview> = input
        .debounce(PREVIEW_DEBOUNCE_MILLIS)
        .distinctUntilChanged()
        .map { value -> tokens.preview(value) }

    private val inputSnapshot: Flow<InputSnapshot> =
        combine(input, previewFlow, submitting) { raw, preview, busy ->
            InputSnapshot(raw = raw, preview = preview, submitting = busy)
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
        combine(inputSnapshot, historySnapshot) { inputState, historyState ->
            TokenUiState(
                input = inputState.raw,
                displayPreview = inputState.preview?.displayValue.orEmpty(),
                isBlank = inputState.preview?.isBlank ?: true,
                failedCheckCount = inputState.preview?.report?.failedChecks?.size ?: 0,
                history = historyState.history,
                meters = historyState.meters,
                selectedMeterId = historyState.selectedMeterId,
                isLoading = false,
                isSubmitting = inputState.submitting,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = TokenUiState(),
        )

    fun onInputChanged(value: String) {
        input.value = value
    }

    /** Restricts history to one meter, or shows every meter when `null`. */
    fun setMeterFilter(meterId: String?) {
        meterFilter.value = meterId
    }

    fun clearInput() {
        input.value = ""
    }

    fun consumeSubmissionResult() {
        result.value = TokenSubmissionResult.Idle
    }

    /**
     * Stores the entered token.
     *
     * A duplicate or a failed check is still stored - the user asked for it to be
     * remembered - but it comes back flagged, and the screen explains why. The
     * field is cleared only after a successful write.
     */
    fun submit() {
        if (submitting.value) return
        if (state.value.isBlank) {
            result.value = TokenSubmissionResult.Rejected("Enter a token value.")
            return
        }

        val raw = input.value
        val meterId = meterFilter.value ?: state.value.selectedMeterId

        viewModelScope.launch {
            submitting.value = true
            try {
                val outcome = tokens.record(raw, meterId)
                result.value = outcome.fold(
                    onSuccess = { TokenSubmissionResult.Stored(it) },
                    onFailure = { error ->
                        TokenSubmissionResult.Rejected(error.tokenRejectionMessage())
                    },
                )
                if (outcome.isSuccess) input.value = ""
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

/** Explains why a token was refused, in the user's terms. */
internal fun DomainError.tokenRejectionMessage(): String = when (this) {
    is DomainError.Validation -> detail
    is DomainError.Storage -> "The token could not be saved. Local storage may be unavailable."
    is DomainError.Unexpected -> "The token could not be saved."
    else -> technicalDetail
}
