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
import androidx.compose.ui.graphics.Brush
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
import com.tanesco.metercompanion.app.designsystem.theme.TanescoTheme

/**
 * The application's one container primitive — now a glass pane.
 *
 * Every screen builds its blocks from [SurfacePanel], which is why upgrading
 * this single primitive upgrades the whole application: translucent gradient
 * fill over the aurora background, a light-catching gradient hairline and a
 * soft green-tinted shadow. It reads as a pane of an instrument, not a web
 * card, because separation comes from light and tone rather than drop-shadow
 * theatre. Public API is unchanged so no screen needed to change.
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
            .glassSurface(RoundedCornerShape(Radii.md))
            .padding(contentPadding),
        content = content,
    )
}

/**
 * A glass panel preceded by a gradient rule in a semantic colour.
 *
 * The rule carries the status; the glass body stays neutral and readable.
 * The rule is now a light-to-deep gradient of the accent so it feels lit
 * from the same source as the panel's top edge.
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
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            Modifier
                .width(ComponentSize.accentRuleWidth + 1.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(Radii.pill))
                .background(
                    Brush.verticalGradient(
                        listOf(accentColor, accentColor.copy(alpha = 0.35f)),
                    ),
                ),
        )
        SurfacePanel(
            modifier = Modifier.weight(1f),
            contentPadding = contentPadding,
            content = content,
        )
    }
}

/**
 * Section heading above groups of content.
 *
 * A short brand-gradient tick + tracked uppercase label. The tick uses the
 * TANESCO green -> sun-yellow relationship; it is identity, not status, which
 * is why it may use the brand highlight that status components may not.
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
        Box(
            Modifier
                .width(22.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(Radii.pill))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            TanescoTheme.glass.brandHighlight,
                        ),
                    ),
                )
                .clearAndSetSemantics { },
        )
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

/** Hairline separator. Marked decorative so screen readers skip it. */
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
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
    )
}

/**
 * A label/value row: the workhorse of the Meter and Settings screens.
 *
 * The value wraps rather than truncating — an account number is compared
 * digit-by-digit against a physical meter, so clipping it would be a
 * data-integrity problem, not a cosmetic one.
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
