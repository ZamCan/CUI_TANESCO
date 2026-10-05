package com.tanesco.metercompanion.app.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tanesco.metercompanion.app.designsystem.theme.ComponentSize
import com.tanesco.metercompanion.app.designsystem.theme.Radii
import com.tanesco.metercompanion.app.designsystem.theme.Spacing

/**
 * The primary action of a screen.
 *
 * Exactly one primary button should be visible at a time. Where a screen appears
 * to have two equally weighted actions, the second is a [SecondaryActionButton]
 * instead - competing filled buttons are how a calm interface becomes a form.
 */
@Composable
fun PrimaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(Radii.sm),
        colors = ButtonDefaults.buttonColors(),
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.md),
        modifier = modifier
            .defaultMinSize(minHeight = ComponentSize.buttonHeight)
            .busySemantics(loading),
    ) {
        ButtonLabel(text = text, loading = loading)
    }
}

/**
 * A secondary action: available, but not the point of the screen.
 */
@Composable
fun SecondaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(Radii.sm),
        contentPadding = PaddingValues(horizontal = Spacing.xl, vertical = Spacing.md),
        modifier = modifier
            .defaultMinSize(minHeight = ComponentSize.buttonHeight)
            .busySemantics(loading),
    ) {
        ButtonLabel(text = text, loading = loading)
    }
}

/**
 * A low-emphasis action, for things such as "Clear" or "Dismiss".
 */
@Composable
fun TertiaryActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(Radii.sm),
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
        modifier = modifier.defaultMinSize(minHeight = ComponentSize.minTouchTarget),
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * A row of actions that wraps to the next line when space runs out.
 *
 * Two buttons with real labels exceed 360dp - the width of a small phone - and
 * clipping a button is never acceptable, so wrapping is the default rather than
 * a special case handled per screen.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(Spacing.md),
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        maxItemsInEachRow = 2,
    ) {
        content()
    }
}

@Composable
private fun ButtonLabel(text: String, loading: Boolean) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = Spacing.md),
        )
    } else {
        Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Announces the busy state while a button is working.
 *
 * The label stays visible so the button does not change width mid-interaction,
 * but a screen reader user is told the action is in progress.
 */
private fun Modifier.busySemantics(loading: Boolean): Modifier = if (loading) {
    semantics { contentDescription = "In progress. Please wait." }
} else {
    this
}
