package com.tanesco.metercompanion.core.diagnostics

import com.tanesco.metercompanion.core.model.EventNames
import com.tanesco.metercompanion.core.repository.EventRepository
import com.tanesco.metercompanion.core.token.TokenFormatProfileRegistry
import com.tanesco.metercompanion.core.token.TokenValidationEngine

/**
 * Reports on the application's own domain services.
 *
 * The token engine check is a real, in-memory round trip through normalisation
 * and the rule pipeline using a fixed probe value. It proves the rules are wired
 * and executable rather than merely constructible, and it touches nothing
 * persistent, so running Diagnostics never writes to the log.
 */
class DomainDiagnosticsProbe(
    private val tokenEngine: TokenValidationEngine,
    private val profiles: TokenFormatProfileRegistry,
    private val eventRepository: EventRepository,
) : DiagnosticProbe {

    override suspend fun run(): List<SubsystemReport> =
        listOf(applicationReport(), offlineReport())

    private suspend fun applicationReport(): SubsystemReport {
        val tokenProbe = probeTokenEngine()

        val checks = listOf(
            DiagnosticCheck(
                id = ID_TOKEN_ENGINE,
                label = "Token engine",
                status = DiagnosticStatus.Ready,
                detail = "${profiles.profiles.size} format profile(s) loaded; structural " +
                    "rules execute locally.",
            ),
            DiagnosticCheck(
                id = ID_TOKEN_PROBE,
                label = "Token rule pipeline",
                status = tokenProbe.status,
                detail = tokenProbe.detail,
                remediation = "A failure here means the rule pipeline is not executable.",
            ),
            DiagnosticCheck(
                id = ID_EVENT_LOGGER,
                label = "Event logger",
                status = probeEventLogger(),
                detail = "${eventRepository.countByName(EventNames.STARTUP_COMPLETED)} " +
                    "startup event(s) recorded in this install.",
            ),
            DiagnosticCheck(
                id = ID_METER_MODEL,
                label = "Meter model",
                status = DiagnosticStatus.Ready,
                detail = "Meter profiles are local records. Live meter data is not modelled " +
                    "because no adapter can supply it.",
            ),
        )

        return SubsystemReport(
            id = SUBSYSTEM_APPLICATION,
            title = "Application",
            summary = "Domain services that run entirely on this device.",
            checks = checks,
        )
    }

    /**
     * Stated explicitly because it is the property that lets this application
     * remain useful today: nothing on any screen needs a network.
     */
    private fun offlineReport(): SubsystemReport = SubsystemReport(
        id = SUBSYSTEM_OFFLINE,
        title = "Offline operation",
        summary = "What this build does with no meter and no network.",
        checks = listOf(
            DiagnosticCheck(
                id = ID_OFFLINE,
                label = "Network independence",
                status = DiagnosticStatus.Ready,
                detail = "No screen requires a network connection. Meter profiles, tokens, " +
                    "activity and settings are stored locally.",
            ),
        ),
    )

    private fun probeTokenEngine(): DiagnosticCheck {
        return try {
            val report = tokenEngine.validate(PROBE_INPUT, profiles.default.id)
            val failed = report.failedChecks
            if (failed.isEmpty()) {
                DiagnosticCheck(
                    id = ID_TOKEN_PROBE,
                    label = "Token rule pipeline",
                    status = DiagnosticStatus.Ready,
                    detail = "Executed ${report.checks.size} rules on a probe value; " +
                        "all passed.",
                )
            } else {
                DiagnosticCheck(
                    id = ID_TOKEN_PROBE,
                    label = "Token rule pipeline",
                    status = DiagnosticStatus.Error,
                    detail = "The shipped probe value failed: " +
                        failed.joinToString("; ") { it.message },
                )
            }
        } catch (throwable: Throwable) {
            DiagnosticCheck(
                id = ID_TOKEN_PROBE,
                label = "Token rule pipeline",
                status = DiagnosticStatus.Error,
                detail = "Threw ${throwable::class.java.simpleName}.",
            )
        }
    }

    private suspend fun probeEventLogger(): DiagnosticStatus = try {
        eventRepository.countByName(EventNames.STARTUP_COMPLETED)
        DiagnosticStatus.Ready
    } catch (throwable: Throwable) {
        DiagnosticStatus.Error
    }

    companion object {
        const val SUBSYSTEM_APPLICATION = "application"
        const val SUBSYSTEM_OFFLINE = "application.offline"

        const val ID_TOKEN_ENGINE = "application.token_engine"
        const val ID_TOKEN_PROBE = "application.token_probe"
        const val ID_EVENT_LOGGER = "application.event_logger"
        const val ID_METER_MODEL = "application.meter_model"
        const val ID_OFFLINE = "application.offline"

        /**
         * A well-formed value that must satisfy the shipped structural profile.
         *
         * If this ever fails, the rule pipeline and the profile disagree, which
         * is a defect rather than a user-facing condition.
         */
        const val PROBE_INPUT = "1234 5678 9012 3456"
    }
}
