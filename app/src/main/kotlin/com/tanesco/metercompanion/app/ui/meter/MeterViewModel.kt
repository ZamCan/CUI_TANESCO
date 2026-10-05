package com.tanesco.metercompanion.app.ui.meter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanesco.metercompanion.connection.ConnectionCoordinator
import com.tanesco.metercompanion.connection.ConnectionRequest
import com.tanesco.metercompanion.connection.ConnectionState
import com.tanesco.metercompanion.core.model.DomainError
import com.tanesco.metercompanion.core.model.EnergySnapshot
import com.tanesco.metercompanion.core.model.MeterProfile
import com.tanesco.metercompanion.core.model.MeterSession
import com.tanesco.metercompanion.core.meter.MeterProfileService
import com.tanesco.metercompanion.core.repository.MeterSessionRepository
import com.tanesco.metercompanion.core.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Everything the Meter screen renders.
 */
data class MeterUiState(
    val isLoading: Boolean = true,
    val meter: MeterProfile? = null,
    val connection: ConnectionState = ConnectionState.Disconnected,
    val lastSession: MeterSession? = null,
    /**
     * Meter-reported data. [EnergySnapshot.Empty] until an adapter supplies
     * something; the screen renders "Not available" for every field of it.
     */
    val snapshot: EnergySnapshot = EnergySnapshot.Empty,
    val connectionAttemptsEnabled: Boolean = false,
) {
    val hasMeter: Boolean get() = meter != null
}

/** Result of a user-initiated connection attempt, for one-shot UI messaging. */
sealed interface ConnectionAttemptResult {
    data object Idle : ConnectionAttemptResult
    data object Succeeded : ConnectionAttemptResult
    data class Refused(val message: String) : ConnectionAttemptResult
}

/**
 * Drives the Meter screen.
 *
 * Note what this class does *not* do: it never manufactures a snapshot. The
 * snapshot starts as [EnergySnapshot.Empty] and is only ever replaced by the
 * result of an adapter call, so the screen cannot display a reading that no
 * meter reported.
 */
class MeterViewModel(
    private val meterProfiles: MeterProfileService,
    private val connectionCoordinator: ConnectionCoordinator,
    private val sessionRepository: MeterSessionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val attemptState = MutableStateFlow<ConnectionAttemptResult>(ConnectionAttemptResult.Idle)
    private val workingState = MutableStateFlow(false)

    val attempt: StateFlow<ConnectionAttemptResult> = attemptState.asStateFlow()

    val isWorking: StateFlow<Boolean> = workingState.asStateFlow()

    val state: StateFlow<MeterUiState> = combine(
        meterProfiles.observeDefaultMeter(),
        connectionCoordinator.state,
        sessionRepository.observeRecent(limit = 1),
        settingsRepository.observe(),
    ) { meter, connection, sessions, settings ->
        MeterUiState(
            isLoading = false,
            meter = meter,
            connection = connection,
            lastSession = sessions.firstOrNull(),
            connectionAttemptsEnabled = settings.allowConnectionAttempts,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = MeterUiState(),
    )

    /**
     * Requests a read from the adapter.
     *
     * The user must have enabled connection attempts in Settings first, so the
     * default experience never implies a meter was reachable. Even when
     * enabled, the honest research adapter reports an unavailable capability,
     * and that message is what the user sees.
     */
    fun requestConnection() {
        if (workingState.value) return
        val meterId = state.value.meter?.id

        viewModelScope.launch {
            workingState.value = true
            attemptState.value = ConnectionAttemptResult.Idle
            try {
                val result = connectionCoordinator.connect(
                    ConnectionRequest(meterId = meterId, userConfirmedProximity = true),
                )
                attemptState.value = result.fold(
                    onSuccess = { ConnectionAttemptResult.Succeeded },
                    onFailure = { error ->
                        ConnectionAttemptResult.Refused(
                            error.detailForConnectionRefusal(),
                        )
                    },
                )
            } finally {
                workingState.value = false
            }
        }
    }

    /** Clears a one-shot attempt result once it has been shown. */
    fun consumeAttemptResult() {
        attemptState.value = ConnectionAttemptResult.Idle
    }

    fun disconnect() {
        viewModelScope.launch {
            workingState.value = true
            try {
                connectionCoordinator.disconnect(state.value.meter?.id)
            } finally {
                workingState.value = false
            }
        }
    }
}

/**
 * Explains a refused connection in the user's terms.
 *
 * Research-pending is described as "not implemented yet" rather than as an
 * error, because nothing went wrong - the capability simply does not exist in
 * this build, and saying "error" would misinform the user.
 */
private fun DomainError.detailForConnectionRefusal(): String =
    when (this) {
        is DomainError.Unavailable -> "$capability is not available in this build. $detail"
        is DomainError.Connection -> detail
        else -> technicalDetail
    }
