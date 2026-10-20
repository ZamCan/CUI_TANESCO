package com.tanesco.metercompanion.app.ui.diagnostics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanesco.metercompanion.core.diagnostics.DiagnosticsReport
import com.tanesco.metercompanion.core.diagnostics.DiagnosticsService
import com.tanesco.metercompanion.app.ui.common.UiState
import com.tanesco.metercompanion.app.ui.common.dataOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Drives the Diagnostics screen.
 *
 * The report is generated on demand rather than kept live: probes touch the
 * database, and regenerating on every composition would make an informational
 * screen the most expensive one in the application.
 */
class DiagnosticsViewModel(
    private val diagnostics: DiagnosticsService,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<DiagnosticsReport>>(UiState.Initial)
    val state: StateFlow<UiState<DiagnosticsReport>> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        refresh()
    }

    /** Re-runs every probe. Safe to call repeatedly; the last run wins. */
    fun refresh() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            if (_state.value is UiState.Ready) {
                // Keep the previous report on screen while re-checking, so the
                // screen never blanks just because it was refreshed.
                _state.value = UiState.Ready(_state.value.dataOrNull() ?: return@launch)
            } else {
                _state.value = UiState.Loading
            }

            val report = runCatching { diagnostics.generate() }
            _state.value = report.fold(
                onSuccess = { UiState.Ready(it) },
                onFailure = { error ->
                    UiState.Failed(
                        com.tanesco.metercompanion.core.model.DomainError.Unexpected(error),
                    )
                },
            )
            _isRefreshing.value = false
        }
    }
}
