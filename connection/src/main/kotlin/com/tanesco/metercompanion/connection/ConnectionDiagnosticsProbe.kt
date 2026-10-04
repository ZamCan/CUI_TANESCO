package com.tanesco.metercompanion.connection

import com.tanesco.metercompanion.core.diagnostics.DiagnosticCheck
import com.tanesco.metercompanion.core.diagnostics.DiagnosticProbe
import com.tanesco.metercompanion.core.diagnostics.DiagnosticStatus
import com.tanesco.metercompanion.core.diagnostics.SubsystemReport

/**
 * Reports the health of the connection boundary for the Diagnostics screen.
 *
 * The report is built from what the adapter *declares*, never from what it
 * claims to have done, and it never probes a meter. When no adapter is
 * registered every row reads [DiagnosticStatus.Unavailable], which is a normal
 * condition rather than a fault.
 */
class ConnectionDiagnosticsProbe(
    private val provider: MeterConnectionProvider,
    private val coordinator: ConnectionCoordinator,
) : DiagnosticProbe {

    override suspend fun run(): List<SubsystemReport> = listOf(adapterReport(), transportReport(), sessionReport())

    private fun adapterReport(): SubsystemReport {
        val connection = provider.current()
        val checks = if (connection == null) {
            listOf(
                DiagnosticCheck(
                    id = ID_ADAPTER,
                    label = "Connection adapter",
                    status = DiagnosticStatus.Unavailable,
                    detail = "No adapter is registered in this build.",
                    remediation = "Register a MeterConnection implementation in the application container.",
                ),
            )
        } else {
            val capabilities = connection.getCapabilities()
            listOf(
                DiagnosticCheck(
                    id = ID_ADAPTER,
                    label = "Connection adapter",
                    status = when (capabilities.availability) {
                        AdapterAvailability.Ready -> DiagnosticStatus.Ready
                        AdapterAvailability.Research -> DiagnosticStatus.Research
                        AdapterAvailability.Unavailable -> DiagnosticStatus.Unavailable
                    },
                    detail = "${capabilities.displayName} is registered.",
                    remediation = when (capabilities.availability) {
                        AdapterAvailability.Research ->
                            "Implementation is pending authorised technical research."

                        else -> null
                    },
                ),
                DiagnosticCheck(
                    id = ID_ADAPTER_CAPABILITIES,
                    label = "Declared meter capabilities",
                    status = if (capabilities.hasAnyMeterCapability) {
                        DiagnosticStatus.Ready
                    } else {
                        DiagnosticStatus.Research
                    },
                    detail = if (capabilities.hasAnyMeterCapability) {
                        "This adapter declares meter operations it can perform."
                    } else {
                        "This adapter declares no meter operations."
                    },
                    remediation = if (capabilities.hasAnyMeterCapability) {
                        null
                    } else {
                        capabilities.notes.firstOrNull()
                    },
                ),
            )
        }

        return SubsystemReport(
            id = SUBSYSTEM_CONNECTION,
            title = "Connection",
            summary = "Boundary for talking to a physical meter.",
            checks = checks,
        )
    }

    private fun transportReport(): SubsystemReport = SubsystemReport(
        id = SUBSYSTEM_TRANSPORT,
        title = "CIU transport",
        summary = "Carries bytes between the device and the customer interface unit.",
        checks = listOf(
            DiagnosticCheck(
                id = ID_CIU_TRANSPORT,
                label = "CIU transport",
                status = DiagnosticStatus.Research,
                detail = "No transport is implemented. The application performs no radio or serial " +
                    "communication with a CIU.",
                remediation = "Requires an authorised specification before implementation.",
            ),
        ),
    )

    private fun sessionReport(): SubsystemReport {
        val state = coordinator.currentState()
        val status = when (state) {
            is ConnectionState.Connected -> DiagnosticStatus.Ready
            is ConnectionState.Connecting -> DiagnosticStatus.Available
            is ConnectionState.Disconnected -> DiagnosticStatus.Available
            is ConnectionState.Unavailable -> DiagnosticStatus.Unavailable
            is ConnectionState.Research -> DiagnosticStatus.Research
            is ConnectionState.Failed -> DiagnosticStatus.Error
        }

        val detail = when (state) {
            is ConnectionState.Connected ->
                "A session is open with a meter."

            is ConnectionState.Connecting ->
                "A connection attempt is in progress."

            is ConnectionState.Disconnected ->
                "No session is open. This is the normal state without a meter."

            is ConnectionState.Unavailable ->
                "No adapter is available: ${state.reason}"

            is ConnectionState.Research ->
                "${state.scope}: ${state.note}"

            is ConnectionState.Failed ->
                "${state.reason} (recoverable: ${state.recoverable})"
        }

        return SubsystemReport(
            id = SUBSYSTEM_SESSION,
            title = "Meter session",
            summary = "A live exchange with one meter.",
            checks = listOf(
                DiagnosticCheck(
                    id = ID_METER_SESSION,
                    label = "Meter session",
                    status = status,
                    detail = detail,
                    remediation = if (status == DiagnosticStatus.Research) {
                        "Becomes available when a connection adapter is implemented."
                    } else {
                        null
                    },
                ),
            ),
        )
    }

    companion object {
        const val SUBSYSTEM_CONNECTION = "connection"
        const val SUBSYSTEM_TRANSPORT = "connection.transport"
        const val SUBSYSTEM_SESSION = "connection.session"

        const val ID_ADAPTER = "connection.adapter"
        const val ID_ADAPTER_CAPABILITIES = "connection.adapter_capabilities"
        const val ID_CIU_TRANSPORT = "connection.ciu_transport"
        const val ID_METER_SESSION = "connection.meter_session"
    }
}
