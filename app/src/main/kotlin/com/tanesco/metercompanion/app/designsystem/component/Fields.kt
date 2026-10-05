package com.tanesco.metercompanion.app.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tanesco.metercompanion.app.designsystem.theme.ComponentSize
import com.tanesco.metercompanion.app.designsystem.theme.Radii
import com.tanesco.metercompanion.app.designsystem.theme.Spacing

/**
 * The application's text field.
 *
 * Every field in every form uses this component so that label placement, error
 * presentation and touch targets are identical throughout. A form assembled
 * from one primitive cannot develop the inconsistencies that make a screen feel
 * unfinished.
 *
 * Error text is always rendered, never swapped in and out: a field that changes
 * height when it becomes invalid moves the buttons below it, which is exactly
 * the moment a user is trying to press one.
 */
@Composable
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    supportingText: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    keyboardCapitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    trailingIcon: @Composable (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = ComponentSize.buttonHeight),
            enabled = enabled,
            label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
            placeholder = placeholder?.let {
                { Text(it, style = MaterialTheme.typography.bodyLarge) }
            },
            isError = errorMessage != null,
            singleLine = singleLine,
            minLines = minLines,
            shape = RoundedCornerShape(Radii.sm),
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction,
                capitalization = keyboardCapitalization,
            ),
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = MaterialTheme.colorScheme.error,
            ),
        )

        val footer = errorMessage ?: supportingText
        if (footer != null) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = if (errorMessage != null) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(
                    start = Spacing.md,
                    top = Spacing.xs,
                    end = Spacing.md,
                ),
            )
        }
    }
}

/**
 * A field for a numeric token or account number.
 *
 * `KeyboardCapitalization.None` plus a numeric keyboard avoids autocorrect
 * mangling a value the user is transcribing from a printed label, and
 * [imeAction] Done on the last field of a form keeps the keyboard from
 * covering the submit button.
 */
@Composable
fun NumericCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    supportingText: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    imeAction: ImeAction = ImeAction.Done,
) {
    LabeledTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier,
        errorMessage = errorMessage,
        supportingText = supportingText,
        placeholder = placeholder,
        enabled = enabled,
        keyboardType = KeyboardType.Number,
        imeAction = imeAction,
        keyboardCapitalization = KeyboardCapitalization.None,
    )
}
