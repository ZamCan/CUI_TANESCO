package com.tanesco.metercompanion.app.ui.metersetup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tanesco.metercompanion.app.designsystem.component.ActionRow
import com.tanesco.metercompanion.app.designsystem.component.LabeledTextField
import com.tanesco.metercompanion.app.designsystem.component.NoticePanel
import com.tanesco.metercompanion.app.designsystem.component.PrimaryActionButton
import com.tanesco.metercompanion.app.designsystem.component.SecondaryActionButton
import com.tanesco.metercompanion.app.designsystem.component.SectionHeader
import com.tanesco.metercompanion.app.designsystem.component.SectionLoadingBar
import com.tanesco.metercompanion.app.designsystem.component.SurfacePanel
import com.tanesco.metercompanion.app.designsystem.theme.Spacing
import com.tanesco.metercompanion.core.model.MeterCategory
import com.tanesco.metercompanion.core.model.PhaseConfiguration
import com.tanesco.metercompanion.core.validation.Fields

/**
 * Meter Setup, used for both creating and editing a profile.
 *
 * Only the identity the customer reads off the physical meter is requested.
 * Nothing here implies the meter has been contacted: the form stores a local
 * record, and the notice below the fields says so explicitly.
 */
@Composable
fun MeterSetupScreen(
    state: MeterSetupUiState,
    onAccountNumberChanged: (String) -> Unit,
    onDisplayNameChanged: (String) -> Unit,
    onRegionChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onCategoryChanged: (MeterCategory) -> Unit,
    onPhaseChanged: (PhaseConfiguration) -> Unit,
    onSave: () -> Unit,
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

        NoticePanel(
            title = "A local record, not a connection",
            body = "These details are stored on this device so the application knows which " +
                "meter you are managing. Nothing is read from or sent to the meter.",
        )

        SurfacePanel {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SectionHeader(title = "Identification")

                LabeledTextField(
                    value = state.draft.accountNumber,
                    onValueChange = onAccountNumberChanged,
                    label = "Account or meter number",
                    errorMessage = state.errorFor(Fields.ACCOUNT_NUMBER),
                    supportingText = "Printed on the meter. Digits, spaces and dashes.",
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    keyboardCapitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
                )

                LabeledTextField(
                    value = state.draft.displayName,
                    onValueChange = onDisplayNameChanged,
                    label = "Name",
                    errorMessage = state.errorFor(Fields.DISPLAY_NAME),
                    supportingText = "A label of your choosing, e.g. \"Kilimanjaro House\".",
                )

                ChoiceGroup(
                    title = "Meter category",
                    options = MeterCategory.entries.toList(),
                    selected = state.draft.category,
                    labelOf = { it.label },
                    onSelect = onCategoryChanged,
                )

                ChoiceGroup(
                    title = "Phase",
                    options = PhaseConfiguration.entries.toList(),
                    selected = state.draft.phase,
                    labelOf = { it.label },
                    onSelect = onPhaseChanged,
                )
            }
        }

        SurfacePanel {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                SectionHeader(title = "Optional")

                LabeledTextField(
                    value = state.draft.region,
                    onValueChange = onRegionChanged,
                    label = "Region or office",
                    errorMessage = state.errorFor(Fields.REGION),
                )

                LabeledTextField(
                    value = state.draft.notes,
                    onValueChange = onNotesChanged,
                    label = "Notes",
                    errorMessage = state.errorFor(Fields.NOTES),
                    singleLine = false,
                    minLines = 3,
                )
            }
        }

        if (state.failureMessage != null) {
            NoticePanel(
                title = "Not saved",
                body = state.failureMessage!!,
            )
        }

        ActionRow {
            PrimaryActionButton(
                text = if (state.isEditing) "Save changes" else "Create meter",
                onClick = onSave,
                enabled = !state.isSaving && !state.isLoading,
                loading = state.isSaving,
                modifier = Modifier.weight(1f),
            )
            SecondaryActionButton(
                text = "Cancel",
                onClick = { onSave() },
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * A wrapping group of chips.
 *
 * Chips rather than a dropdown: there are three or four options, all visible at
 * once, which is faster than opening a menu on a phone.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceGroup(
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
