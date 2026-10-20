package com.tanesco.metercompanion.app.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tanesco.metercompanion.app.designsystem.component.EmptyState
import com.tanesco.metercompanion.app.designsystem.component.HairlineDivider
import com.tanesco.metercompanion.app.designsystem.component.SectionLoadingBar
import com.tanesco.metercompanion.app.designsystem.component.StatusPill
import com.tanesco.metercompanion.app.designsystem.component.SurfacePanel
import com.tanesco.metercompanion.app.designsystem.theme.IdentifierTextStyle
import com.tanesco.metercompanion.app.designsystem.theme.Spacing
import com.tanesco.metercompanion.core.diagnostics.DiagnosticStatus
import com.tanesco.metercompanion.core.model.AppEvent
import com.tanesco.metercompanion.core.model.EventSeverity
import com.tanesco.metercompanion.core.time.formatIso8601Utc

/**
 * The Activity screen: the application's local audit trail.
 *
 * Timestamps are rendered as UTC ISO-8601 rather than a relative "2 minutes
 * ago". For an audit log, an absolute timestamp that matches what was stored is
 * worth more than a friendly relative form, and it avoids implying a clock
 * comparison the phone may not support.
 */
@Composable
fun ActivityScreen(
    state: ActivityUiState,
    onFilterSelected: (ActivityFilter) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                top = contentPadding.calculateTopPadding() + Spacing.sm,
                bottom = contentPadding.calculateBottomPadding() + Spacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                FilterRow(
                    selected = state.activeFilter,
                    onSelect = onFilterSelected,
                )
            }

            if (state.isLoading) {
                item { SectionLoadingBar(label = "Loading activity…") }
            }

            if (!state.isLoading && state.events.isEmpty()) {
                item {
                    EmptyState(
                        title = if (state.activeFilter == ActivityFilter.All) {
                            "Nothing recorded yet"
                        } else {
                            "No ${state.activeFilter.label.lowercase()} activity"
                        },
                        body = "Every action this application performs is written here with a " +
                            "timestamp, so the state shown on any screen can be traced back to " +
                            "something that actually happened.",
                    )
                }
            }

            items(state.events, key = { it.id }) { event ->
                EventRow(event = event)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterRow(selected: ActivityFilter, onSelect: (ActivityFilter) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        ActivityFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = { Text(filter.label) },
            )
        }
    }
}

@Composable
private fun EventRow(event: AppEvent) {
    SurfacePanel(contentPadding = PaddingValues(Spacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    text = event.summary,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                StatusPill(
                    label = event.severity.label,
                    status = event.severity.toDiagnosticStatus(),
                )
            }

            HairlineDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    text = formatIso8601Utc(event.occurredAtMillis),
                    style = IdentifierTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = event.category.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = event.name,
                style = IdentifierTextStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            event.detail?.let { detail ->
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun EventSeverity.toDiagnosticStatus(): DiagnosticStatus = when (this) {
    EventSeverity.Info -> DiagnosticStatus.Available
    EventSeverity.Success -> DiagnosticStatus.Ready
    EventSeverity.Warning -> DiagnosticStatus.Research
    EventSeverity.Error -> DiagnosticStatus.Error
}
