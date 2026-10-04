package com.tanesco.metercompanion.connection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The only adapter that ships with this build.
 *
 * It performs **no** meter communication of any kind. It exists so that the
 * connection boundary is a real, exercised code path rather than an empty
 * interface, and it reports the truthful state for every operation:
 * "this is not implemented, because it requires technical research we have not
 * done".
 *
 * What this class deliberately does *not* do:
 *
 *  - speak, guess at, or reverse engineer any CIU or meter protocol
 *  - simulate a successful session, a reading, or a token transfer
 *  - accept, store or derive meter authentication material
 *
 * Replacing it with a genuine adapter is a registration change in the
 * application's dependency container, not a change to any screen or service.
 * See docs/connection/CONNECTION-BOUNDARY.md.
 */
class ResearchMeterConnection : MeterConnection {

    override val id: String = ADAPTER_ID

    override fun getCapabilities(): ConnectionCapabilities = CAPABILITIES

    private val mutableState = MutableStateFlow<ConnectionState>(
        ConnectionState.Disconnected,
    )

    override fun getConnectionState(): ConnectionState = mutableState.value

    override fun observeState(): StateFlow<ConnectionState> = mutableState.asStateFlow()

    override suspend fun connect(request: ConnectionRequest): ConnectionResult<ConnectionSession> {
        // No transport is engaged and none is simulated. The state stays
        // Disconnected because Disconnected is the truth: no session exists.
        mutableState.value = ConnectionState.Disconnected
        return ConnectionResult.Failure(
            ConnectionFailure.ResearchPending(
                code = CODE_RESEARCH,
                capability = CAPABILITY_SESSION,
                scope = SCOPE,
                note = NOTE,
            ),
        )
    }

    override suspend fun disconnect(): ConnectionResult<Unit> {
        mutableState.value = ConnectionState.Disconnected
        return ConnectionResult.Success(Unit)
    }

    override suspend fun readMeterState(): ConnectionResult<MeterSnapshot> =
        ConnectionResult.Failure(
            ConnectionFailure.ResearchPending(
                code = CODE_RESEARCH,
                capability = CAPABILITY_METER_STATE,
                scope = SCOPE,
                note = NOTE,
            ),
        )

    companion object {
        const val ADAPTER_ID = "research-placeholder"

        /** Stable code recorded in the event log and in diagnostics. */
        const val CODE_RESEARCH = "connection.research_pending"

        private const val SCOPE = "CIU transport and meter session"
        private const val CAPABILITY_SESSION = "meter session"
        private const val CAPABILITY_METER_STATE = "meter state read"
        private const val NOTE =
            "Reading from or writing to a physical meter requires technical " +
                "research that has not been completed. No communication is attempted."

        /**
         * Declares no meter capability at all.
         *
         * Every flag is `false`, which is what drives the Meter screen to show
         * unavailable actions instead of controls that would fail.
         */
        val CAPABILITIES: ConnectionCapabilities = ConnectionCapabilities(
            id = ADAPTER_ID,
            displayName = "Research placeholder adapter",
            availability = AdapterAvailability.Research,
            canReadMeterIdentity = false,
            canReadElectricalMeasurements = false,
            canReadTokenCredit = false,
            canSubmitToken = false,
            requiresPhysicalProximity = true,
            notes = listOf(
                "No meter communication is implemented in this build.",
                "Capabilities become available only after authorised research " +
                    "and hardware testing are complete.",
            ),
        )
    }
}
