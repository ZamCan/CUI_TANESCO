package com.tanesco.metercompanion.app.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tanesco.metercompanion.app.designsystem.theme.ComponentSize
import com.tanesco.metercompanion.app.designsystem.theme.IdentifierTextStyle
import com.tanesco.metercompanion.app.designsystem.theme.Radii
import com.tanesco.metercompanion.app.designsystem.theme.Spacing

/**
 * The application's one container primitive.
 *
 * Panels are separated by a hairline outline and a barely-there tonal
 * difference rather than by shadow. Shadows read as "floating card" and quickly
 * turn a screen into a web dashboard; a hairline reads as a panel of an
 * instrument and stays quiet at any elevation.
 *
 * Deliberately not built on [androidx.compose.material3.Card]: Card brings a
 * container colour and elevation model this design does not use, and having a
 * single primitive prevents the drift that appears when several near-identical
 * containers accumulate.
 */
@Composable
fun SurfacePanel(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.md))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * A panel preceded by a vertical rule in a semantic colour.
 *
 * Used to attach meaning to a block - a connection state, a warning - without
 * tinting the whole surface. Colour carries the status; the body stays neutral
 * and readable.
 */
@Composable
fun AccentPanel(
    accentColor: Color,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    content: @Composable ColumnScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // Intrinsic height lets the rule match the panel's height exactly,
            // without either side having to know the other's size.
            .height(IntrinsicSize.Min),
    ) {
        Box(
            Modifier
                .width(ComponentSize.accentRuleWidth)
                .fillMaxHeight()
                .background(accentColor),
        )
        SurfacePanel(
            modifier = Modifier.weight(1f),
            contentPadding = contentPadding,
            content = content,
        )
    }
}

/**
 * Section heading used above groups of content within a screen.
 *
 * Uppercase with wide tracking appears only here, at small sizes, because this is
 * the one place a screen needs to signal "a new category begins".
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = Spacing.md, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/**
 * Hairline separator.
 *
 * Marked decorative so screen readers do not announce a line of nothing.
 */
@Composable
fun HairlineDivider(
    modifier: Modifier = Modifier,
    inset: Dp = 0.dp,
) {
    HorizontalDivider(
        modifier = modifier
            .padding(start = inset)
            .clearAndSetSemantics { },
        thickness = ComponentSize.hairline,
        color = MaterialTheme.colorScheme.outline,
    )
}

/**
 * A label/value row: the workhorse of the Meter and Settings screens.
 *
 * The value wraps rather than truncating. An account number is compared
 * digit-by-digit against a physical meter, so silently clipping one would be a
 * genuine data-integrity problem, not a cosmetic one.
 */
@Composable
fun LabeledValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    monospaceValue: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = value,
            style = if (monospaceValue) IdentifierTextStyle else MaterialTheme.typography.bodyLarge,
            color = valueColor,
            modifier = Modifier
                .weight(0.58f)
                .semantics { contentDescription = "$label: $value" },
        )
    }
}
