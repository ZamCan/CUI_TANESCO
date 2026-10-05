package com.tanesco.metercompanion.core.diagnostics

import com.tanesco.metercompanion.core.time.Clock
import kotlinx.coroutines.CancellationException

/**
 * Aggregates [DiagnosticProbe]s into a single report.
 *
 * Two properties matter here:
 *
 *  1. **Isolation.** A probe that throws produces an `Error` row for that probe
 *     only. Diagnostics must still render, because a screen that crashes while
 *     reporting a fault is the worst possible outcome.
 *  2. **Honesty.** Probes report observed state. The service never upgrades a
 *     status, and never invents a passing check for something it did not test.
 */
class DiagnosticsService(
    private val probes: List<DiagnosticProbe>,
    private val clock: Clock,
) {

    suspend fun generate(): DiagnosticsReport {
        // A probe owns one *or more* subsystem groups, so results are flattened
        // rather than mapped one-to-one.
        val subsystems = probes.flatMap { probe -> runProbe(probe) }
        return DiagnosticsReport(
            generatedAtMillis = clock.nowMillis(),
            subsystems = subsystems,
        )
    }

    private suspend fun runProbe(probe: DiagnosticProbe): List<SubsystemReport> = try {
        probe.run()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (throwable: Throwable) {
        // The exception *type* is reported, never its message: a platform
        // exception message can carry data that must not be displayed.
        listOf(
            SubsystemReport(
                id = SUBSYSTEM_UNKNOWN,
                title = "Unnamed subsystem",
                summary = "A diagnostic probe failed to run.",
                checks = listOf(
                    DiagnosticCheck(
                        id = "diagnostics.probe_failure",
                        label = "Diagnostic probe",
                        status = DiagnosticStatus.Error,
                        detail = "Probe threw ${throwable::class.java.simpleName}.",
                        remediation = "This is a defect; a probe should handle its own failures.",
                    ),
                ),
            ),
        )
    }

    companion object {
        const val SUBSYSTEM_UNKNOWN = "unknown"
    }
}
