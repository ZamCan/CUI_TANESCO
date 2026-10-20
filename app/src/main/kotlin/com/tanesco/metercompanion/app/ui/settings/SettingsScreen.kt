package com.tanesco.metercompanion.app.ui.settings

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.tanesco.metercompanion.app.designsystem.component.HairlineDivider
import com.tanesco.metercompanion.app.designsystem.component.LabeledValueRow
import com.tanesco.metercompanion.app.designsystem.component.NavigationRow
import com.tanesco.metercompanion.app.designsystem.component.NoticePanel
import com.tanesco.metercompanion.app.designsystem.component.PanelDivider
import com.tanesco.metercompanion.app.designsystem.component.SectionHeader
import com.tanesco.metercompanion.app.designsystem.component.SelectableRow
import com.tanesco.metercompanion.app.designsystem.component.SurfacePanel
import com.tanesco.metercompanion.app.designsystem.component.TertiaryActionButton
import com.tanesco.metercompanion.app.designsystem.theme.Spacing
import com.tanesco.metercompanion.core.model.AppLanguage
import com.tanesco.metercompanion.core.model.AppearanceMode
import com.tanesco.metercompanion.core.model.MeterProfile
import com.tanesco.metercompanion.core.model.UnitSystem

/**
 * Settings.
 *
 * Groups are ordered by how often they matter: meters first, then appearance,
 * then the connection switch, then local data. The connection switch is off by
 * default and carries an explicit explanation, because turning it on must not be
 * mistaken for being able to reach a meter.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    notice: SettingsNotice,
    onAppearanceChanged: (AppearanceMode) -> Unit,
    onUnitSystemChanged: (UnitSystem) -> Unit,
    onConnectionAttemptsChanged: (Boolean) -> Unit,
    onDeveloperModeChanged: (Boolean) -> Unit,
    onDefaultMeterChanged: (String?) -> Unit,
    onArchiveMeter: (String) -> Unit,
    onAddMeter: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenAbout: () -> Unit,
    onNoticeConsumed: () -> Unit,
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
        when (notice) {
            is SettingsNotice.Saved -> NoticePanel(
                title = "Saved",
                body = notice.message,
                status = com.tanesco.metercompanion.core.diagnostics.DiagnosticStatus.Ready,
                action = { TertiaryActionButton(text = "Dismiss", onClick = onNoticeConsumed) },
            )

            is SettingsNotice.Failed -> NoticePanel(
                title = "Not saved",
                body = notice.message,
                status = com.tanesco.metercompanion.core.diagnostics.DiagnosticStatus.Error,
                action = { TertiaryActionButton(text = "Dismiss", onClick = onNoticeConsumed) },
            )

            SettingsNotice.Idle -> Unit
        }

        MetersSection(
            meters = state.meters,
            selectedId = state.settings.defaultMeterId,
            onSelect = onDefaultMeterChanged,
            onArchive = onArchiveMeter,
            onAddMeter = onAddMeter,
        )

        AppearanceSection(
            appearance = state.settings.appearance,
            unitSystem = state.settings.unitSystem,
            language = state.settings.language,
            onAppearanceChanged = onAppearanceChanged,
            onUnitSystemChanged = onUnitSystemChanged,
        )

        ConnectionSection(
            attemptsEnabled = state.settings.allowConnectionAttempts,
            onChanged = onConnectionAttemptsChanged,
            onOpenDiagnostics = onOpenDiagnostics,
        )

        DeveloperSection(
            developerMode = state.settings.developerMode,
            onChanged = onDeveloperModeChanged,
        )

        AboutSection(onOpenAbout = onOpenAbout)
    }
}

@Composable
private fun MetersSection(
    meters: List<MeterProfile>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    onArchive: (String) -> Unit,
    onAddMeter: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionHeader(title = "Meters")
        SurfacePanel {
            Column {
                if (meters.isEmpty()) {
                    Text(
                        text = "No meters configured.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = Spacing.sm),
                    )
                } else {
                    meters.forEachIndexed { index, meter ->
                        SelectableRow(
                            title = meter.displayName,
                            subtitle = "${meter.accountNumber} · ${meter.subtitle}",
                            selected = meter.id == selectedId,
                            onSelect = { onSelect(meter.id) },
                            trailing = {
                                TertiaryActionButton(
                                    text = "Archive",
                                    onClick = { onArchive(meter.id) },
                                )
                            },
                        )
                        if (index != meters.lastIndex) HairlineDivider()
                    }
                }
                HairlineDivider()
                NavigationRow(
                    title = "Add a meter",
                    subtitle = "Record another account or meter number",
                    onClick = onAddMeter,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSection(
    appearance: AppearanceMode,
    unitSystem: UnitSystem,
    language: AppLanguage,
    onAppearanceChanged: (AppearanceMode) -> Unit,
    onUnitSystemChanged: (UnitSystem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionHeader(title = "Appearance and units")
        SurfacePanel {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ChipGroup(
                    title = "Theme",
                    options = AppearanceMode.entries,
                    selected = appearance,
                    labelOf = { it.label },
                    onSelect = onAppearanceChanged,
                )
                HairlineDivider()
                ChipGroup(
                    title = "Energy units",
                    options = UnitSystem.entries,
                    selected = unitSystem,
                    labelOf = { it.label },
                    onSelect = onUnitSystemChanged,
                )
                HairlineDivider()
                LabeledValueRow(
                    label = "Language",
                    // Only reviewed translations are offered. See ADR 0007.
                    value = AppLanguage.System.label,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChipGroup(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(labelOf(option)) },
                )
            }
        }
    }
}

@Composable
private fun ConnectionSection(
    attemptsEnabled: Boolean,
    onChanged: (Boolean) -> Unit,
    onOpenDiagnostics: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionHeader(title = "Meter connection")
        SurfacePanel {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ToggleRow(
                    title = "Allow connection attempts",
                    subtitle = "Off by default. When enabled, the application will ask the " +
                        "connection layer to try; it will still report that no meter " +
                        "communication is implemented.",
                    checked = attemptsEnabled,
                    onCheckedChange = onChanged,
                )
                HairlineDivider()
                NavigationRow(
                    title = "Diagnostics",
                    subtitle = "What the connection layer currently supports",
                    onClick = onOpenDiagnostics,
                )
            }
        }
    }
}

@Composable
private fun DeveloperSection(developerMode: Boolean, onChanged: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionHeader(title = "Developer")
        SurfacePanel {
            Column {
                ToggleRow(
                    title = "Developer details",
                    subtitle = "Show additional technical information in Diagnostics.",
                    checked = developerMode,
                    onCheckedChange = onChanged,
                )
            }
        }
    }
}

@Composable
private fun AboutSection(onOpenAbout: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionHeader(title = "About")
        SurfacePanel(contentPadding = PaddingValues(Spacing.sm)) {
            Column {
                NavigationRow(
                    title = "Version and technical information",
                    onClick = onOpenAbout,
                )
            }
        }
    }
}

/**
 * A labelled switch.
 *
 * The whole row is the toggle's accessible target and announces its own state,
 * so a screen reader user does not have to find the switch separately from its
 * explanation.
 */
@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title. $subtitle"
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** Kept for the divider used between settings groups on wide layouts. */
@Suppress("unused")
@Composable
private fun GroupDivider() = PanelDivider()
