package com.tanesco.metercompanion.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tanesco.metercompanion.connection.ConnectionCoordinator
import com.tanesco.metercompanion.connection.ConnectionState
import com.tanesco.metercompanion.core.diagnostics.DiagnosticStatus
import com.tanesco.metercompanion.core.model.AppEvent
import com.tanesco.metercompanion.core.model.EventQuery
import com.tanesco.metercompanion.core.model.MeterProfile
import com.tanesco.metercompanion.core.model.TokenQuery
import com.tanesco.metercompanion.core.meter.MeterProfileService
import com.tanesco.metercompanion.core.repository.EventRepository
import com.tanesco.metercompanion.core.repository.TokenRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Everything Home renders.
 *
 * A single immutable state object rather than several independent flows: Home
 * combines five sources, and separate flows would let the screen briefly show a
 * meter next to a connection state belonging to a different one.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val meter: MeterProfile? = null,
    val connection: ConnectionState = ConnectionState.Disconnected,
    val recentEvents: List<AppEvent> = emptyList(),
    val tokenCount: Int = 0,
    val hasAnyMeter: Boolean = false,
) {
    /** True when there is nothing at all configured yet. */
    val isUnconfigured: Boolean get() = !hasAnyMeter && !isLoading
}

/**
 * Maps a [ConnectionState] onto the diagnostics vocabulary the status components
 * use.
 *
 * This mapping lives in the presentation layer rather than in the connection
 * module because it is a *design* decision - how a transport state is presented
 * - and not a property of the transport.
 */
fun ConnectionState.toDiagnosticStatus(): DiagnosticStatus = when (this) {
    is ConnectionState.Connected -> DiagnosticStatus.Ready
    is ConnectionState.Connecting -> DiagnosticStatus.Available
    is ConnectionState.Disconnected -> DiagnosticStatus.Available
    is ConnectionState.Unavailable -> DiagnosticStatus.Unavailable
    is ConnectionState.Research -> DiagnosticStatus.Research
    is ConnectionState.Failed -> DiagnosticStatus.Error
}

/**
 * The sentence shown under the connection status on Home and Meter.
 *
 * Every branch states a fact. In particular, no branch claims a meter was
 * reached.
 */
fun ConnectionState.explanation(): String = when (this) {
    is ConnectionState.Connected ->
        "A session with a meter is open."

    is ConnectionState.Connecting ->
        "Attempting to open a session. This may take a moment."

    is ConnectionState.Disconnected ->
        "No session is open. This is the normal state when you are not at the meter."

    is ConnectionState.Unavailable ->
        reason

    is ConnectionState.Research ->
        "$scope. $note"

    is ConnectionState.Failed ->
        if (recoverable) "$reason You can try again." else reason
}

/**
 * Builds the Home screen's state from the domain.
 */
class HomeViewModel(
    private val meterProfiles: MeterProfileService,
    private val connectionCoordinator: ConnectionCoordinator,
    eventRepository: EventRepository,
    tokenRepository: TokenRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        meterProfiles.observeDefaultMeter(),
        meterProfiles.observeAll(),
        connectionCoordinator.state,
        eventRepository.observe(EventQuery(limit = RECENT_EVENT_LIMIT)),
        tokenRepository.observeHistory(TokenQuery(limit = 1)),
    ) { defaultMeter, allMeters, connection, events, tokens ->
        HomeUiState(
            isLoading = false,
            meter = defaultMeter,
            connection = connection,
            recentEvents = events,
            tokenCount = tokens.totalCount,
            hasAnyMeter = allMeters.any { !it.isArchived },
        )
    }.stateIn(
        scope = viewModelScope,
        // Keeps the flow warm briefly so that returning to Home does not flash
        // a loading state, while still releasing resources when the app is
        // backgrounded for a long time.
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = HomeUiState(),
    )

    private companion object {
        const val RECENT_EVENT_LIMIT = 5
    }
}
