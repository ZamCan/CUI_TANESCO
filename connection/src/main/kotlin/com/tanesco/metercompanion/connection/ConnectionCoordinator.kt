package com.tanesco.metercompanion.connection

import com.tanesco.metercompanion.core.model.AppEvent
import com.tanesco.metercompanion.core.model.DomainError
import com.tanesco.metercompanion.core.model.DomainResult
import com.tanesco.metercompanion.core.model.EventCategory
import com.tanesco.metercompanion.core.model.EventNames
import com.tanesco.metercompanion.core.model.EventSeverity
import com.tanesco.metercompanion.core.model.MeterSession
import com.tanesco.metercompanion.core.model.SessionOutcome
import com.tanesco.metercompanion.core.repository.EventRepository
import com.tanesco.metercompanion.core.repository.MeterSessionRepository
import com.tanesco.metercompanion.core.time.Clock
import com.tanesco.metercompanion.core.time.IdGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The application's only entry point to the meter connection boundary.
 *
 * Responsibilities:
 *
 *  - resolve the active adapter through [MeterConnectionProvider]
 *  - translate [ConnectionFailure] into [DomainError]
 *  - record a [MeterSession] for every attempt, successful or not
 *  - append an [AppEvent] so the Activity screen reflects what really happened
 *
 * What it must never do: paper over a failure. Every code path either returns a
 * success backed by adapter confirmation, or returns a failure and writes that
 * failure to the log.
 */
class ConnectionCoordinator(
    private val provider: MeterConnectionProvider,
    private val sessionRepository: MeterSessionRepository,
    private val eventRepository: EventRepository,
    private val clock: Clock,
    private val idGenerator: IdGenerator,
    /**
     * Long-lived scope used to mirror adapter state. It must outlive any single
     * screen; the application container owns it.
     */
    private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow<ConnectionState>(initialState())

    /** The connection state as the UI should see it. */
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private var mirrorJob: Job? = null

    /**
     * Starts mirroring the active adapter's state into [state].
     *
     * Safe to call again after a new adapter is registered; the previous mirror
     * is cancelled first so that state can never be written by two adapters at
     * once.
     */
    fun bindAdapter() {
        mirrorJob?.cancel()
        val adapterState = provider.current()?.observeState()
        if (adapterState == null) {
            _state.value = initialState()
            return
        }
        _state.value = adapterState.value
        mirrorJob = scope.launch {
            adapterState.collect { _state.value = it }
        }
    }

    /**
     * The state to show before - or without - an adapter.
     *
     * No adapter is a supported build condition, so it is reported as
     * [ConnectionState.Unavailable] rather than as a failure.
     */
    private fun initialState(): ConnectionState =
        if (provider.current() == null) {
            ConnectionState.Unavailable(
                code = CODE_NO_ADAPTER,
                reason = "No connection adapter is registered in this build.",
            )
        } else {
            ConnectionState.Disconnected
        }

    fun capabilities(): ConnectionCapabilities? = provider.current()?.getCapabilities()

    fun currentState(): ConnectionState = state.value

    /** Recent attempts, newest first. */
    fun recentSessions(limit: Int = 20): Flow<List<MeterSession>> = sessionRepository.observeRecent(limit)

    /**
     * Requests a connection.
     *
     * Records the attempt either way. Returns a [DomainResult.Failure] whose
     * code is the adapter's stable code, for example
     * `connection.research_pending`.
     */
    suspend fun connect(request: ConnectionRequest): DomainResult<ConnectionSession> {
        val connection = provider.current()
        if (connection == null) {
            val failure = ConnectionFailure.Unavailable(
                code = CODE_NO_ADAPTER,
                capability = "meter connection",
                reason = "No connection adapter is registered in this build.",
            )
            recordUnavailable(request.meterId, failure)
            return DomainResult.Failure(failure.toDomainError())
        }

        val sessionId = idGenerator.newId()
        val startedAt = clock.nowMillis()

        eventRepository.append(
            event(
                name = EventNames.CONNECTION_ATTEMPTED,
                severity = EventSeverity.Info,
                summary = "Connection requested",
                meterId = request.meterId,
                userInitiated = true,
                detail = "adapter '${connection.id}'",
            ),
        )

        return when (val result = connection.connect(request)) {
            is ConnectionResult.Success -> {
                val session = MeterSession(
                    id = sessionId,
                    meterId = request.meterId,
                    startedAtMillis = startedAt,
                    endedAtMillis = null,
                    outcome = SessionOutcome.Completed,
                    adapterId = connection.id,
                )
                sessionRepository.start(session)
                eventRepository.append(
                    event(
                        name = EventNames.CONNECTION_SESSION_CLOSED,
                        severity = EventSeverity.Success,
                        summary = "Meter session established",
                        meterId = request.meterId,
                        detail = "adapter '${connection.id}'",
                    ),
                )
                DomainResult.Success(result.value)
            }

            is ConnectionResult.Failure -> {
                val outcome = when (result.failure) {
                    is ConnectionFailure.ResearchPending -> SessionOutcome.Unavailable
                    is ConnectionFailure.UnsupportedOperation -> SessionOutcome.Unavailable
                    is ConnectionFailure.Unavailable -> SessionOutcome.Unavailable
                    is ConnectionFailure.PermissionDenied -> SessionOutcome.Refused
                    else -> SessionOutcome.Failed
                }
                sessionRepository.start(
                    MeterSession(
                        id = sessionId,
                        meterId = request.meterId,
                        startedAtMillis = startedAt,
                        endedAtMillis = clock.nowMillis(),
                        outcome = outcome,
                        adapterId = connection.id,
                        failureCode = result.failure.code,
                    ),
                )
                recordFailure(request.meterId, result.failure)
                DomainResult.Failure(result.failure.toDomainError())
            }
        }
    }

    /** Asks the adapter to read meter data. Never fabricates a reading. */
    suspend fun readMeterState(meterId: String?): DomainResult<MeterSnapshot> {
        val connection = provider.current()
            ?: return DomainResult.Failure(
                ConnectionFailure.Unavailable(
                    code = CODE_NO_ADAPTER,
                    capability = "meter state read",
                    reason = "No connection adapter is registered in this build.",
                ).toDomainError(),
            )

        eventRepository.append(
            event(
                name = EventNames.CONNECTION_READ_REQUESTED,
                severity = EventSeverity.Info,
                summary = "Meter data read requested",
                meterId = meterId,
                detail = "adapter '${connection.id}'",
            ),
        )

        return when (val result = connection.readMeterState()) {
            is ConnectionResult.Success -> DomainResult.Success(result.value)
            is ConnectionResult.Failure -> {
                recordFailure(meterId, result.failure)
                DomainResult.Failure(result.failure.toDomainError())
            }
        }
    }

    suspend fun disconnect(meterId: String? = null): DomainResult<Unit> {
        val connection = provider.current()
            ?: return DomainResult.Failure(
                DomainError.Unavailable(
                    code = CODE_NO_ADAPTER,
                    capability = "meter connection",
                    detail = "No connection adapter is registered in this build.",
                ),
            )

        return when (val result = connection.disconnect()) {
            is ConnectionResult.Success -> {
                eventRepository.append(
                    event(
                        name = EventNames.CONNECTION_DISCONNECTED,
                        severity = EventSeverity.Info,
                        summary = "Disconnected",
                        meterId = meterId,
                        detail = "adapter '${connection.id}'",
                    ),
                )
                DomainResult.Success(Unit)
            }

            is ConnectionResult.Failure -> DomainResult.Failure(result.failure.toDomainError())
        }
    }

    private suspend fun recordUnavailable(meterId: String?, failure: ConnectionFailure) {
        eventRepository.append(
            event(
                name = EventNames.CONNECTION_UNAVAILABLE,
                severity = EventSeverity.Warning,
                summary = "Connection unavailable",
                meterId = meterId,
                detail = failure.reasonOrCode(),
            ),
        )
    }

    private suspend fun recordFailure(meterId: String?, failure: ConnectionFailure) {
        val (name, severity, summary) = when (failure) {
            is ConnectionFailure.ResearchPending -> Triple(
                EventNames.CONNECTION_RESEARCH_PENDING,
                EventSeverity.Info,
                "Meter communication not implemented",
            )

            is ConnectionFailure.UnsupportedOperation -> Triple(
                EventNames.CONNECTION_UNSUPPORTED,
                EventSeverity.Warning,
                "Capability not supported by this adapter",
            )

            is ConnectionFailure.Unavailable -> Triple(
                EventNames.CONNECTION_UNAVAILABLE,
                EventSeverity.Warning,
                "Connection unavailable",
            )

            is ConnectionFailure.PermissionDenied -> Triple(
                EventNames.CONNECTION_DENIED,
                EventSeverity.Warning,
                "Permission not granted",
            )

            is ConnectionFailure.InvalidRequest -> Triple(
                EventNames.CONNECTION_FAILED,
                EventSeverity.Warning,
                "Connection request rejected",
            )

            is ConnectionFailure.TransportError -> Triple(
                EventNames.CONNECTION_FAILED,
                EventSeverity.Error,
                "Connection failed",
            )

            is ConnectionFailure.Internal -> Triple(
                EventNames.CONNECTION_FAILED,
                EventSeverity.Error,
                "Connection adapter error",
            )
        }

        eventRepository.append(
            event(
                name = name,
                severity = severity,
                summary = summary,
                meterId = meterId,
                detail = failure.reasonOrCode(),
                userInitiated = true,
            ),
        )
    }

    private fun ConnectionFailure.reasonOrCode(): String = when (this) {
        is ConnectionFailure.Unavailable -> reason
        is ConnectionFailure.ResearchPending -> "$scope: $note"
        is ConnectionFailure.TransportError -> reason
        is ConnectionFailure.InvalidRequest -> reason
        is ConnectionFailure.PermissionDenied -> "permission '$permission' was not granted"
        is ConnectionFailure.UnsupportedOperation -> reason
        is ConnectionFailure.Internal -> reason
    }

    private fun event(
        name: String,
        severity: EventSeverity,
        summary: String,
        meterId: String?,
        detail: String? = null,
        userInitiated: Boolean = false,
    ): AppEvent = AppEvent.create(
        id = idGenerator.newId(),
        category = EventCategory.Connection,
        name = name,
        severity = severity,
        summary = summary,
        clock = clock,
        detail = detail,
        meterId = meterId,
        userInitiated = userInitiated,
    )

    companion object {
        const val CODE_NO_ADAPTER = "connection.no_adapter"
    }
}
