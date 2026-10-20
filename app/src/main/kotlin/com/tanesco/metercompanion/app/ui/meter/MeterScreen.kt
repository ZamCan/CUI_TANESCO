package com.tanesco.metercompanion.app.ui.meter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tanesco.metercompanion.app.designsystem.component.AccentPanel
import com.tanesco.metercompanion.app.designsystem.component.ActionRow
import com.tanesco.metercompanion.app.designsystem.component.EmptyState
import com.tanesco.metercompanion.app.designsystem.component.HairlineDivider
import com.tanesco.metercompanion.app.designsystem.component.LabeledValueRow
import com.tanesco.metercompanion.app.designsystem.component.MeasurementGrid
import com.tanesco.metercompanion.app.designsystem.component.MeasurementTile
import com.tanesco.metercompanion.app.designsystem.component.NoticePanel
import com.tanesco.metercompanion.app.designsystem.component.PrimaryActionButton
import com.tanesco.metercompanion.app.designsystem.component.SecondaryActionButton
import com.tanesco.metercompanion.app.designsystem.component.SectionHeader
import com.tanesco.metercompanion.app.designsystem.component.SectionLoadingBar
import com.tanesco.metercompanion.app.designsystem.component.StatusExplainer
import com.tanesco.metercompanion.app.designsystem.component.SurfacePanel
import com.tanesco.metercompanion.app.designsystem.component.style
import com.tanesco.metercompanion.app.designsystem.theme.Spacing
import com.tanesco.metercompanion.app.ui.home.explanation
import com.tanesco.metercompanion.app.ui.home.toDiagnosticStatus
import com.tanesco.metercompanion.core.model.Units

/**
 * The Meter screen.
 *
 * Structure follows the order in which a user standing at a meter needs
 * information: which meter, is it connected, what does it read, what happened
 * last.
 *
 * Every measurement tile is fed from a nullable measurement, so with no adapter
 * the grid renders a row of explicit "Not available" states rather than zeros.
 */
@Composable
fun MeterScreen(
    state: MeterUiState,
    isWorking: Boolean,
    attempt: ConnectionAttemptResult,
    onAddMeter: () -> Unit,
    onEditMeter: () -> Unit,
    onRequestConnection: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onAttemptConsumed: () -> Unit,
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
        if (state.isLoading) {
            SectionLoadingBar(label = "Loading meter…")
        }

        if (!state.hasMeter) {
            EmptyState(
                title = "No meter configured",
                body = "Add the account or meter number printed on your meter to see " +
                    "its identity, readings and connection state here.",
                action = {
                    PrimaryActionButton(text = "Add a meter", onClick = onAddMeter)
                },
            )
            return@Column
        }

        MeterIdentityCard(state = state, onEditMeter = onEditMeter)

        ConnectionSection(
            state = state,
            isWorking = isWorking,
            attempt = attempt,
            onRequestConnection = onRequestConnection,
            onOpenDiagnostics = onOpenDiagnostics,
            onAttemptConsumed = onAttemptConsumed,
        )

        ReadingsSection(state = state)
    }
}

@Composable
private fun MeterIdentityCard(state: MeterUiState, onEditMeter: () -> Unit) {
    val meter = state.meter ?: return
    SurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SectionHeader(
                title = "Meter identity",
                trailing = {
                    SecondaryActionButton(
                        text = "Edit",
                        onClick = onEditMeter,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                },
            )
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
            LabeledValueRow(label = "Category", value = meter.category.label)
            LabeledValueRow(label = "Phase", value = meter.phase.label)
            meter.region?.let { region ->
                LabeledValueRow(label = "Region", value = region)
            }
            meter.notes?.let { notes ->
                LabeledValueRow(label = "Notes", value = notes)
            }
        }
    }
}

@Composable
private fun ConnectionSection(
    state: MeterUiState,
    isWorking: Boolean,
    attempt: ConnectionAttemptResult,
    onRequestConnection: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onAttemptConsumed: () -> Unit,
) {
    val status = state.connection.toDiagnosticStatus()

    AccentPanel(accentColor = status.style().accent) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            SectionHeader(title = "Connection")
            StatusExplainer(
                title = state.connection.label,
                status = status,
                explanation = state.connection.explanation(),
            )

            when (attempt) {
                is ConnectionAttemptResult.Refused -> {
                    // Honest, specific copy: the capability does not exist in
                    // this build, which is not the same as a failure.
                    NoticePanel(
                        title = "Not available in this build",
                        body = attempt.message,
                        status = status,
                    )
                    SecondaryActionButton(
                        text = "Dismiss",
                        onClick = onAttemptConsumed,
                    )
                }

                ConnectionAttemptResult.Succeeded -> {
                    NoticePanel(
                        title = "Session established",
                        body = "The adapter reported an open session with a meter.",
                        status = com.tanesco.metercompanion.core.diagnostics.DiagnosticStatus.Ready,
                    )
                    SecondaryActionButton(
                        text = "Dismiss",
                        onClick = onAttemptConsumed,
                    )
                }

                ConnectionAttemptResult.Idle -> Unit
            }

            HairlineDivider()

            if (!state.connectionAttemptsEnabled) {
                Text(
                    text = "Connection attempts are turned off. Enable them in Settings if you " +
                        "want the application to ask the connection layer to try.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            ActionRow {
                PrimaryActionButton(
                    text = "Request read",
                    onClick = onRequestConnection,
                    enabled = state.connectionAttemptsEnabled && !isWorking,
                    loading = isWorking,
                    modifier = Modifier.weight(1f),
                )
                SecondaryActionButton(
                    text = "Diagnostics",
                    onClick = onOpenDiagnostics,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * Meter-reported values.
 *
 * With no adapter every tile shows "Not available" and the reason. The section
 * header states up front that nothing has been read, so a user does not have to
 * infer it from six identical tiles.
 */
@Composable
private fun ReadingsSection(state: MeterUiState) {
    val snapshot = state.snapshot
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionHeader(title = "Readings")
        SurfacePanel {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(
                    text = if (snapshot.isEmpty) {
                        "No values have been read from a meter in this build. The fields below " +
                            "are placeholders for readings a connection adapter will supply."
                    } else {
                        "Values below were reported by the meter."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HairlineDivider()
                MeasurementGrid {
                    item {
                        MeasurementTile("Voltage", snapshot.voltage, unavailableReason = NO_READING)
                    }
                    item {
                        MeasurementTile("Current", snapshot.current, unavailableReason = NO_READING)
                    }
                    item {
                        MeasurementTile("Power", snapshot.power, unavailableReason = NO_READING)
                    }
                    item {
                        MeasurementTile("Energy", snapshot.activeEnergy, unavailableReason = NO_READING)
                    }
                    item {
                        MeasurementTile("Power factor", snapshot.powerFactor, unavailableReason = NO_READING)
                    }
                    item {
                        MeasurementTile("Frequency", snapshot.frequency, unavailableReason = NO_READING)
                    }
                }
                HairlineDivider()
                LabeledValueRow(
                    label = "Units",
                    value = "${Units.VOLTS}, ${Units.AMPERES}, ${Units.WATTS}, ${Units.KILOWATT_HOURS}",
                )
            }
        }
    }
}

private const val NO_READING =
    "No meter connection is established, so this value has not been read."

/** Kept for previews: a screen with no meter shows the empty state above. */
@Suppress("unused")
private val previewPadding = PaddingValues(0.dp)
