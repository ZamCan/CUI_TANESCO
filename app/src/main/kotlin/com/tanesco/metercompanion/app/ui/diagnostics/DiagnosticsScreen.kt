package com.tanesco.metercompanion.app.ui.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tanesco.metercompanion.app.designsystem.component.ErrorState
import com.tanesco.metercompanion.app.designsystem.component.HairlineDivider
import com.tanesco.metercompanion.app.designsystem.component.LabeledValueRow
import com.tanesco.metercompanion.app.designsystem.component.SecondaryActionButton
import com.tanesco.metercompanion.app.designsystem.component.SectionHeader
import com.tanesco.metercompanion.app.designsystem.component.SectionLoadingBar
import com.tanesco.metercompanion.app.designsystem.component.StatusDot
import com.tanesco.metercompanion.app.designsystem.component.StatusPill
import com.tanesco.metercompanion.app.designsystem.component.SurfacePanel
import com.tanesco.metercompanion.app.designsystem.theme.Spacing
import com.tanesco.metercompanion.app.ui.common.UiState
import com.tanesco.metercompanion.app.ui.common.userMessage
import com.tanesco.metercompanion.core.diagnostics.DiagnosticStatus
import com.tanesco.metercompanion.core.diagnostics.DiagnosticsReport
import com.tanesco.metercompanion.core.diagnostics.SubsystemReport
import com.tanesco.metercompanion.core.time.formatIso8601Utc

/**
 * Diagnostics: what is working, what is not, and what is deliberately absent.
 *
 * The five statuses are the point of this screen. `Research` exists so that
 * "we have not built this" is never displayed as either a fault or a success -
 * a distinction the user cannot otherwise infer from a generic error.
 */
@Composable
fun DiagnosticsScreen(
    state: UiState<DiagnosticsReport>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = Spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        when (state) {
            is UiState.Initial, is UiState.Loading -> SectionLoadingBar(label = "Running checks…")

            is UiState.Failed -> ErrorState(
                title = "Diagnostics could not run",
                body = state.error.userMessage(),
                onRetry = onRefresh,
            )

            is UiState.Ready -> {
                val report = state.data
                SummaryPanel(report = report, isRefreshing = isRefreshing, onRefresh = onRefresh)
                report.subsystems.forEach { subsystem ->
                    SubsystemPanel(subsystem)
                }
            }
        }
    }
}

@Composable
private fun SummaryPanel(
    report: DiagnosticsReport,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
) {
    SurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SectionHeader(title = "Overall")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                StatusPill(
                    label = "Status",
                    status = report.overallStatus,
                )
                Text(
                    text = "${report.totalChecks} check(s), ${report.problemCount} problem(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LabeledValueRow(
                label = "Generated",
                value = formatIso8601Utc(report.generatedAtMillis),
                monospaceValue = true,
            )
            SecondayActionButtonRow(isRefreshing = isRefreshing, onRefresh = onRefresh)
        }
    }
}

@Composable
private fun SecondayActionButtonRow(isRefreshing: Boolean, onRefresh: () -> Unit) {
    SecondaryActionButton(
        text = "Re-run checks",
        onClick = onRefresh,
        enabled = !isRefreshing,
        loading = isRefreshing,
    )
}

/**
 * One subsystem group.
 *
 * Each row states a status, a one-sentence observation, and - where it helps -
 * what would make the check pass.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubsystemPanel(subsystem: SubsystemReport) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionHeader(title = subsystem.title)
        SurfacePanel {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = subsystem.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HairlineDivider()

                subsystem.checks.forEachIndexed { index, check ->
                    CheckRow(
                        label = check.label,
                        status = check.status,
                        detail = check.detail,
                        remediation = check.remediation,
                    )
                    if (index != subsystem.checks.lastIndex) HairlineDivider()
                }
            }
        }
    }
}

@Composable
private fun CheckRow(
    label: String,
    status: DiagnosticStatus,
    detail: String,
    remediation: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            StatusDot(status = status)
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            StatusPill(label = label, status = status)
        }
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.lg),
        )
        if (remediation != null) {
            Text(
                text = remediation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.lg),
            )
        }
    }
}

/** Filter chips are unused on this screen; kept out of the public surface. */
@Suppress("unused")
private val unusedFilterApi = FlowRow::class

@Suppress("unused")
private val unusedPadding = PaddingValues(0.dp)
