package com.tanesco.metercompanion.core.diagnostics

/**
 * Health of one component of the application.
 *
 * The five states are chosen so that "we have not built this yet" is
 * visually and programmatically distinct from "this is broken":
 *
 *  - [READY]       implemented, initialised and working
 *  - [AVAILABLE]   implemented and usable, but currently idle
 *  - [RESEARCH]    deliberately not implemented; requires further study
 *  - [UNAVAILABLE] cannot run on this device or in this configuration
 *  - [ERROR]       a defect; needs investigation
 */
enum class DiagnosticStatus(val label: String, val isProblem: Boolean) {
    Ready("Ready", false),
    Available("Available", false),
    Research("Research", false),
    Unavailable("Unavailable", false),
    Error("Error", true),
    ;

    /** Sort weight so the most significant rows appear first. */
    val severityRank: Int
        get() = when (this) {
            Error -> 0
            Unavailable -> 1
            Research -> 2
            Ready -> 3
            Available -> 4
        }
}

/** One checkable component. */
data class DiagnosticCheck(
    /** Stable identifier, e.g. `connection.ciu_transport`. */
    val id: String,
    val label: String,
    val status: DiagnosticStatus,
    /** One sentence explaining the observed state. */
    val detail: String,
    /** What would make this check pass, when that is meaningful. */
    val remediation: String? = null,
)

/** A named group of checks. */
data class SubsystemReport(
    val id: String,
    val title: String,
    /** One line describing the group as a whole. */
    val summary: String,
    val checks: List<DiagnosticCheck>,
) {
    /** The most significant status present in this group. */
    val overallStatus: DiagnosticStatus
        get() = checks.minByOrNull { it.status.severityRank }?.status ?: DiagnosticStatus.Available

    val problemCount: Int get() = checks.count { it.status.isProblem }
}

/** The complete diagnostics result rendered by the Diagnostics screen. */
data class DiagnosticsReport(
    val generatedAtMillis: Long,
    val subsystems: List<SubsystemReport>,
) {
    val totalChecks: Int get() = subsystems.sumOf { it.checks.size }

    val problemCount: Int get() = subsystems.sumOf { it.problemCount }

    val overallStatus: DiagnosticStatus
        get() = subsystems.minByOrNull { it.overallStatus.severityRank }?.overallStatus
            ?: DiagnosticStatus.Available
}

/**
 * A single named probe contributing to the diagnostics report.
 *
 * Probes are supplied by whichever layer owns the component: the database probe
 * comes from `:data`, the connection probe from `:connection`. The core only
 * knows how to aggregate them.
 */
fun interface DiagnosticProbe {
    suspend fun run(): List<SubsystemReport>
}
