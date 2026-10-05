package com.tanesco.metercompanion.app.ui.token

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.ui.text.style.TextAlign
import com.tanesco.metercompanion.app.designsystem.component.ActionRow
import com.tanesco.metercompanion.app.designsystem.component.EmptyState
import com.tanesco.metercompanion.app.designsystem.component.HairlineDivider
import com.tanesco.metercompanion.app.designsystem.component.NoticePanel
import com.tanesco.metercompanion.app.designsystem.component.NumericCodeField
import com.tanesco.metercompanion.app.designsystem.component.PrimaryActionButton
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
 * The most important thing this screen communicates is the *limit* of what it
 * does. It records tokens and runs structural checks on them. It cannot tell
 * the user a token is valid, because only a meter can do that and no meter is
 * reachable. That caveat is stated in the panel above the field rather than
 * hidden behind a help screen.
 */
@Composable
fun TokenScreen(
    state: TokenUiState,
    submission: TokenSubmissionResult,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onClearInput: () -> Unit,
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
                NoticePanel(
                    title = "Local checks only",
                    body = "Tokens entered here are stored on this device and checked for " +
                        "formatting problems. Nothing is sent to a meter, and a token that " +
                        "passes these checks has not been confirmed by a meter.",
                )
            }

            item {
                TokenEntryCard(
                    state = state,
                    submission = submission,
                    onInputChanged = onInputChanged,
                    onSubmit = onSubmit,
                    onClearInput = onClearInput,
                    onSubmissionConsumed = onSubmissionConsumed,
                )
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
                        body = "Tokens you enter are kept here with the local check results " +
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

@Composable
private fun TokenEntryCard(
    state: TokenUiState,
    submission: TokenSubmissionResult,
    onInputChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onClearInput: () -> Unit,
    onSubmissionConsumed: () -> Unit,
) {
    SurfacePanel {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            SectionHeader(title = "Enter a token")

            NumericCodeField(
                value = state.input,
                onValueChange = onInputChanged,
                label = "Token",
                placeholder = "20 digits",
                supportingText = when {
                    state.isBlank -> "Separate the digits with spaces if that is easier to read."
                    state.failedCheckCount > 0 ->
                        "Does not match the expected format. Check the digits and try again."

                    else -> "Formatted as: ${state.displayPreview}"
                },
                errorMessage = if (state.failedCheckCount > 0 && !state.isBlank) {
                    "Does not match the expected format."
                } else {
                    null
                },
            )

            when (submission) {
                is TokenSubmissionResult.Stored -> {
                    StoredTokenNotice(record = submission.record, onDismiss = onSubmissionConsumed)
                }

                is TokenSubmissionResult.Rejected -> {
                    NoticePanel(
                        title = "Not recorded",
                        body = submission.message,
                        status = DiagnosticStatus.Error,
                        action = {
                            TertiaryActionButton(text = "Dismiss", onClick = onSubmissionConsumed)
                        },
                    )
                }

                TokenSubmissionResult.Idle -> Unit
            }

            ActionRow {
                PrimaryActionButton(
                    text = "Record token",
                    onClick = onSubmit,
                    enabled = !state.isBlank && !state.isSubmitting,
                    loading = state.isSubmitting,
                    modifier = Modifier.weight(1f),
                )
                TertiaryActionButton(
                    text = "Clear",
                    onClick = onClearInput,
                    enabled = state.input.isNotEmpty() && !state.isSubmitting,
                )
            }
        }
    }
}

/**
 * Confirmation after a token is stored.
 *
 * Copy states exactly what happened - it was recorded locally and what the local
 * checks said. It never says the token was accepted, sent or applied.
 */
@Composable
private fun StoredTokenNotice(record: TokenRecord, onDismiss: () -> Unit) {
    NoticePanel(
        title = "Recorded locally",
        body = when (record.status) {
            TokenStatus.StructurallyValid ->
                "Stored on this device and passed the local format checks. " +
                    "It has not been sent to a meter."

            TokenStatus.Rejected ->
                "Stored on this device, but it failed the local format checks: " +
                    record.validation?.failedChecks?.joinToString(" ") { it.message }.orEmpty()

            TokenStatus.Duplicate ->
                "Stored on this device. It matches a token recorded earlier, which is often " +
                    "a sign it was entered twice."

            TokenStatus.Recorded ->
                "Stored on this device. No local checks have been run yet."
        },
        status = when (record.status) {
            TokenStatus.StructurallyValid -> DiagnosticStatus.Ready
            TokenStatus.Rejected -> DiagnosticStatus.Error
            TokenStatus.Duplicate -> DiagnosticStatus.Research
            TokenStatus.Recorded -> DiagnosticStatus.Available
        },
        action = { TertiaryActionButton(text = "Dismiss", onClick = onDismiss) },
    )
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
                textAlign = TextAlign.Start,
            )

            record.validation?.let { report ->
                Text(
                    text = "${report.checks.size} local check(s), " +
                        "${report.failedChecks.size} failed",
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
