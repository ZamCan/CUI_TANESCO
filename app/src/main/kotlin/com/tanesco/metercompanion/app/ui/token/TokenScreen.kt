package com.tanesco.metercompanion.app.ui.token

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tanesco.metercompanion.app.designsystem.component.EmptyState
import com.tanesco.metercompanion.app.designsystem.component.HairlineDivider
import com.tanesco.metercompanion.app.designsystem.component.NumericCodeField
import com.tanesco.metercompanion.app.designsystem.component.NoticePanel
import com.tanesco.metercompanion.app.designsystem.component.SectionHeader
import com.tanesco.metercompanion.app.designsystem.component.StatusPill
import com.tanesco.metercompanion.app.designsystem.component.SurfacePanel
import com.tanesco.metercompanion.app.designsystem.component.TertiaryActionButton
import com.tanesco.metercompanion.app.designsystem.theme.Spacing
import com.tanesco.metercompanion.app.designsystem.theme.TokenValueTextStyle
import com.tanesco.metercompanion.core.diagnostics.DiagnosticStatus
import com.tanesco.metercompanion.core.model.MeterProfile
import com.tanesco.metercompanion.core.model.TokenRecord
import com.tanesco.metercompanion.core.model.TokenStatus

/**
 * The Token Center.
 *
 * Entry is presented as the meter's own front panel: the same keypad layout, the
 * same indicator positions, the same CANCEL and ENTER keys. A keyboard entry
 * field is offered underneath for users who prefer typing, and for assistive
 * technology, writing into the same digit buffer.
 */
@Composable
fun TokenScreen(
    state: TokenUiState,
    submission: TokenSubmissionResult,
    onDigit: (Char) -> Unit,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    onPageNext: () -> Unit,
    onPagePrevious: () -> Unit,
    onTypedValueChanged: (String) -> Unit,
    onMeterFilterChanged: (String?) -> Unit,
    onSubmissionConsumed: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item {
                SurfacePanel {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        MeterEntryExplanation()
                        HairlineDivider()
                        MeterTokenEntryPanel(
                            keypad = state.keypad,
                            failedCheckCount = state.failedCheckCount,
                            accountLabel = state.selectedMeterId
                                ?.let { selectedLabel(state.meters, it) }
                                ?: "NO METER",
                            onDigit = onDigit,
                            onCancel = onCancel,
                            onEnter = onSubmit,
                            onPageNext = onPageNext,
                            onPagePrevious = onPagePrevious,
                            isSubmitting = state.isSubmitting,
                        )
                    }
                }
            }

            item {
                KeyboardAlternative(
                    value = state.typedValue,
                    enabled = !state.isSubmitting,
                    failedCheckCount = state.failedCheckCount,
                    isBlank = state.isBlank,
                    onValueChange = onTypedValueChanged,
                    onCancel = onCancel,
                )
            }

            if (submission !is TokenSubmissionResult.Idle) {
                item {
                    SubmissionNotice(
                        submission = submission,
                        onDismiss = onSubmissionConsumed,
                    )
                }
            }

            if (state.meters.size > 1) {
                item {
                    MeterFilterRow(
                        meters = state.meters,
                        selectedId = state.selectedMeterId,
                        onSelect = onMeterFilterChanged,
                    )
                }
            }

            item {
                SectionHeader(
                    title = "Token history",
                    trailing = {
                        Text(
                            text = "${state.history.records.size} of ${state.history.totalCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            if (state.history.records.isEmpty()) {
                item {
                    EmptyState(
                        title = "No tokens recorded",
                        body = "Tokens you enter are kept here with their local check results " +
                            "and a timestamp, so you can review what has been recorded.",
                    )
                }
            } else {
                items(state.history.records, key = { it.id }) { record ->
                    TokenHistoryRow(record = record)
                }
            }
        }
    }
}

private fun selectedLabel(meters: List<MeterProfile>, meterId: String): String =
    meters.firstOrNull { it.id == meterId }?.accountNumber ?: meterId.takeLast(6)

/**
 * Keyboard entry.
 *
 * Kept as a full alternative rather than a convenience, because a keypad
 * reproduced in software is still a set of 48dp touch targets: a user with a
 * switch device, a stylus or motor tremor is better served by a real text field.
 * Both paths write into the same buffer.
 */
@Composable
private fun KeyboardAlternative(
    value: String,
    enabled: Boolean,
    isBlank: Boolean,
    failedCheckCount: Int,
    onValueChange: (String) -> Unit,
    onCancel: () -> Unit,
) {
    SurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SectionHeader(title = "Enter with a keyboard instead")
            NumericCodeField(
                value = value,
                onValueChange = onValueChange,
                label = "Token",
                placeholder = "20 digits",
                enabled = enabled,
                supportingText = if (isBlank) {
                    "Spaces and dashes are accepted and removed."
                } else {
                    "${value.length} digits entered"
                },
                errorMessage = if (failedCheckCount > 0 && !isBlank) {
                    "Does not match the expected format."
                } else {
                    null
                },
            )
            TertiaryActionButton(text = "Clear entry", onClick = onCancel, enabled = !isBlank)
        }
    }
}

/**
 * Result of pressing ENTER.
 *
 * Copy states exactly what happened. It never says a token was accepted, sent or
 * applied, because no adapter can report that.
 */
@Composable
private fun SubmissionNotice(
    submission: TokenSubmissionResult,
    onDismiss: () -> Unit,
) {
    when (submission) {
        is TokenSubmissionResult.Stored -> NoticePanel(
            title = "Recorded locally",
            body = when (submission.record.status) {
                TokenStatus.StructurallyValid ->
                    "Saved on this device and passed the local format checks. " +
                        "It has not been sent to a meter."

                TokenStatus.Rejected ->
                    "Saved on this device, but it failed the local format checks: " +
                        submission.record.validation?.failedChecks
                            ?.joinToString(" ") { it.message }.orEmpty()

                TokenStatus.Duplicate ->
                    "Saved on this device. It matches a token recorded earlier, which is " +
                        "often a sign it was entered twice."

                TokenStatus.Recorded ->
                    "Saved on this device. No local checks have been run yet."
            },
            status = when (submission.record.status) {
                TokenStatus.StructurallyValid -> DiagnosticStatus.Ready
                TokenStatus.Rejected -> DiagnosticStatus.Error
                TokenStatus.Duplicate -> DiagnosticStatus.Research
                TokenStatus.Recorded -> DiagnosticStatus.Available
            },
            action = { TertiaryActionButton(text = "Dismiss", onClick = onDismiss) },
        )

        is TokenSubmissionResult.Rejected -> NoticePanel(
            title = "Not recorded",
            body = submission.message,
            status = DiagnosticStatus.Error,
            action = { TertiaryActionButton(text = "Dismiss", onClick = onDismiss) },
        )

        TokenSubmissionResult.Idle -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeterFilterRow(
    meters: List<MeterProfile>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        SectionHeader(title = "Show history for")
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            FilterChip(
                selected = selectedId == null,
                onClick = { onSelect(null) },
                label = { Text("All meters") },
            )
            meters.forEach { meter ->
                FilterChip(
                    selected = selectedId == meter.id,
                    onClick = { onSelect(meter.id) },
                    label = { Text(meter.displayName) },
                )
            }
        }
    }
}

@Composable
private fun TokenHistoryRow(record: TokenRecord) {
    SurfacePanel(contentPadding = PaddingValues(Spacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Text(
                    text = record.displayValue,
                    style = TokenValueTextStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                StatusPill(
                    label = record.status.label,
                    status = record.status.toDiagnosticStatus(),
                )
            }

            HairlineDivider()

            Text(
                text = "Hand-off: ${record.transferState.label}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            record.validation?.let { report ->
                Text(
                    text = "${report.checks.size} local check(s), ${report.failedChecks.size} failed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Token status reuses the shared status vocabulary for consistent colour. */
internal fun TokenStatus.toDiagnosticStatus(): DiagnosticStatus = when (this) {
    TokenStatus.StructurallyValid -> DiagnosticStatus.Ready
    TokenStatus.Rejected -> DiagnosticStatus.Error
    TokenStatus.Duplicate -> DiagnosticStatus.Research
    TokenStatus.Recorded -> DiagnosticStatus.Available
}

/** Kept so the screen's minimum width expectation is explicit for previews. */
internal val MinimumSupportedWidth = 320.dp
