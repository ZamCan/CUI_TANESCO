package com.tanesco.metercompanion.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tanesco.metercompanion.app.designsystem.component.AccentPanel
import com.tanesco.metercompanion.app.designsystem.component.EmptyState
import com.tanesco.metercompanion.app.designsystem.component.HairlineDivider
import com.tanesco.metercompanion.app.designsystem.component.LabeledValueRow
import com.tanesco.metercompanion.app.designsystem.component.NavigationRow
import com.tanesco.metercompanion.app.designsystem.component.PrimaryActionButton
import com.tanesco.metercompanion.app.designsystem.component.SecondaryActionButton
import com.tanesco.metercompanion.app.designsystem.component.SectionHeader
import com.tanesco.metercompanion.app.designsystem.component.StatusExplainer
import com.tanesco.metercompanion.app.designsystem.component.StatusPill
import com.tanesco.metercompanion.app.designsystem.component.SurfacePanel
import com.tanesco.metercompanion.app.designsystem.component.style
import com.tanesco.metercompanion.app.designsystem.theme.Spacing
import com.tanesco.metercompanion.connection.ConnectionState
import com.tanesco.metercompanion.core.diagnostics.DiagnosticStatus
import com.tanesco.metercompanion.core.model.AppEvent
import com.tanesco.metercompanion.core.model.EventSeverity

/**
 * Home: the answer to "what is happening, and what can I do?".
 *
 * Order is deliberate. Status first, because that is what a user opens the
 * application to find; actions second; history last, because it is reference
 * material rather than a call to action.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenMeter: () -> Unit,
    onOpenTokens: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenAbout: () -> Unit,
    onAddMeter: () -> Unit,
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
        Spacer(Modifier.height(Spacing.xs))

        ApplicationStatusCard(
            isLoading = state.isLoading,
            hasMeter = state.hasAnyMeter,
            tokenCount = state.tokenCount,
        )

        if (state.isUnconfigured) {
            WelcomePanel(onAddMeter = onAddMeter)
        } else {
            ConfiguredMeterCard(state = state, onOpenMeter = onOpenMeter)
        }

        ConnectionCard(state = state, onOpenDiagnostics = onOpenDiagnostics)

        ShortcutsCard(
            onOpenMeter = onOpenMeter,
            onOpenTokens = onOpenTokens,
            onOpenActivity = onOpenActivity,
        )

        RecentActivityCard(
            events = state.recentEvents,
            isLoading = state.isLoading,
            onOpenActivity = onOpenActivity,
        )

        MoreCard(
            onOpenSettings = onOpenSettings,
            onOpenDiagnostics = onOpenDiagnostics,
            onOpenAbout = onOpenAbout,
        )

        Spacer(Modifier.height(Spacing.xxl))
    }
}

/**
 * Whether the application itself is working.
 *
 * Kept separate from the meter: the application is healthy with no meter
 * configured, and conflating the two would make a working app look broken.
 */
@Composable
private fun ApplicationStatusCard(isLoading: Boolean, hasMeter: Boolean, tokenCount: Int) {
    SurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SectionHeader(title = "Application")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                StatusPill(
                    label = "Local data",
                    status = if (isLoading) DiagnosticStatus.Available else DiagnosticStatus.Ready,
                )
                Text(
                    text = if (isLoading) "Loading" else "Working offline",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LabeledValueRow(
                label = "Meters configured",
                value = if (hasMeter) "1" else "0",
            )
            LabeledValueRow(
                label = "Tokens recorded",
                value = tokenCount.toString(),
            )
        }
    }
}

/**
 * Shown on a fresh install: explains what the application does and what to do
 * next, instead of presenting an empty dashboard.
 */
@Composable
private fun WelcomePanel(onAddMeter: () -> Unit) {
    SurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Text(
                text = "No meter configured yet",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Add the account or meter number printed on your meter. " +
                    "Everything is stored on this device, and every feature works " +
                    "without a network connection.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryActionButton(
                text = "Add a meter",
                onClick = onAddMeter,
                modifier = Modifier.widthIn(max = 320.dp),
            )
        }
    }
}

@Composable
private fun ConfiguredMeterCard(state: HomeUiState, onOpenMeter: () -> Unit) {
    val meter = state.meter
    SurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SectionHeader(
                title = "Configured meter",
                trailing = {
                    SecondaryActionButton(
                        text = "Open",
                        onClick = onOpenMeter,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                },
            )
            if (meter == null) {
                Text(
                    text = "No meter selected",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HairlineDivider()
                SecondaryActionButton(text = "Add a meter", onClick = onOpenMeter)
            } else {
                Text(
                    text = meter.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                LabeledValueRow(
                    label = "Account",
                    value = meter.accountNumber,
                    monospaceValue = true,
                )
                LabeledValueRow(label = "Type", value = meter.subtitle)
                meter.region?.let { region ->
                    LabeledValueRow(label = "Region", value = region)
                }
            }
        }
    }
}

/**
 * The connection state together with the reason it is in that state.
 *
 * This is the screen's most important honesty statement: it never implies a
 * meter was reached, and it points at Diagnostics so the reason can be verified.
 */
@Composable
private fun ConnectionCard(state: HomeUiState, onOpenDiagnostics: () -> Unit) {
    val status = state.connection.toDiagnosticStatus()
    AccentPanel(accentColor = status.style().accent) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SectionHeader(title = "Meter connection")
            StatusExplainer(
                title = state.connection.label,
                status = status,
                explanation = state.connection.explanation(),
            )
            HairlineDivider()
            SecondaryActionButton(text = "View diagnostics", onClick = onOpenDiagnostics)
        }
    }
}

@Composable
private fun ShortcutsCard(
    onOpenMeter: () -> Unit,
    onOpenTokens: () -> Unit,
    onOpenActivity: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionHeader(title = "Shortcuts")
        SurfacePanel(contentPadding = PaddingValues(Spacing.sm)) {
            Column {
                ShortcutRow(
                    icon = Icons.Filled.Settings,
                    title = "Meter",
                    subtitle = "Identity, readings and connection state",
                    onClick = onOpenMeter,
                )
                HairlineDivider()
                ShortcutRow(
                    icon = Icons.Filled.Lock,
                    title = "Token Center",
                    subtitle = "Enter and review tokens",
                    onClick = onOpenTokens,
                )
                HairlineDivider()
                ShortcutRow(
                    icon = Icons.Filled.List,
                    title = "Activity",
                    subtitle = "Everything this application has recorded",
                    onClick = onOpenActivity,
                )
            }
        }
    }
}

@Composable
private fun ShortcutRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    NavigationRow(
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        leading = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        },
        trailing = { RowChevron() },
    )
}

@Composable
private fun RowChevron() {
    Icon(
        imageVector = Icons.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun RecentActivityCard(
    events: List<AppEvent>,
    isLoading: Boolean,
    onOpenActivity: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionHeader(
            title = "Recent activity",
            trailing = {
                if (events.isNotEmpty()) {
                    SecondaryActionButton(text = "See all", onClick = onOpenActivity)
                }
            },
        )
        SurfacePanel {
            when {
                isLoading && events.isEmpty() -> Text(
                    text = "Loading activity…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                events.isEmpty() -> EmptyState(
                    title = "Nothing recorded yet",
                    body = "Actions you take here are logged, so every state this " +
                        "application shows can be traced back to something that happened.",
                )

                else -> Column {
                    events.forEachIndexed { index, event ->
                        ActivityRow(event = event)
                        if (index != events.lastIndex) HairlineDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityRow(event: AppEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = event.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = event.category.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusPill(label = event.severity.label, status = event.severity.toDiagnosticStatus())
    }
}

/** Severity reuses the status vocabulary so one colour meaning exists per role. */
private fun EventSeverity.toDiagnosticStatus(): DiagnosticStatus = when (this) {
    EventSeverity.Info -> DiagnosticStatus.Available
    EventSeverity.Success -> DiagnosticStatus.Ready
    EventSeverity.Warning -> DiagnosticStatus.Research
    EventSeverity.Error -> DiagnosticStatus.Error
}

@Composable
private fun MoreCard(
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionHeader(title = "More")
        SurfacePanel(contentPadding = PaddingValues(Spacing.sm)) {
            Column {
                NavigationRow(
                    title = "Settings",
                    subtitle = "Meters, appearance, units and local data",
                    onClick = onOpenSettings,
                    trailing = { RowChevron() },
                )
                HairlineDivider()
                NavigationRow(
                    title = "Diagnostics",
                    subtitle = "Subsystem health and connection support",
                    onClick = onOpenDiagnostics,
                    trailing = { RowChevron() },
                )
                HairlineDivider()
                NavigationRow(
                    title = "About",
                    subtitle = "Version and technical information",
                    onClick = onOpenAbout,
                    trailing = { RowChevron() },
                )
            }
        }
    }
}

/** Exposed for tests: the research state must never render as an error. */
internal fun researchStateIsInformational(state: ConnectionState): Boolean =
    state is ConnectionState.Research
