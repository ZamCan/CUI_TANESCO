package com.tanesco.metercompanion.app.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tanesco.metercompanion.app.designsystem.theme.ComponentSize
import com.tanesco.metercompanion.app.designsystem.theme.MeasurementValueTextStyle
import com.tanesco.metercompanion.app.designsystem.theme.Radii
import com.tanesco.metercompanion.app.designsystem.theme.Spacing
import com.tanesco.metercompanion.core.model.Measurement

/**
 * Displays one electrical quantity - or states plainly that it is unavailable.
 *
 * This component is the single most important honesty guarantee in the user
 * interface. It accepts a nullable [Measurement] and has no way to render a
 * value that did not come from a meter:
 *
 *  - a measurement renders its value, unit and a "read from meter" marker
 *  - `null` renders "Not available" and an explanation of why
 *
 * There is deliberately no overload that takes a raw number, so a future
 * contributor cannot introduce a placeholder reading by accident.
 */
@Composable
fun MeasurementTile(
    label: String,
    measurement: Measurement<Double>?,
    modifier: Modifier = Modifier,
    unavailableReason: String = "No meter connection is established, so this value has not been read.",
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.sm))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(
                width = ComponentSize.hairline,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(Radii.sm),
            )
            .padding(Spacing.md)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (measurement == null) {
            Text(
                text = "Not available",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = unavailableReason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatMeasurement(measurement.value),
                    style = MeasurementValueTextStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = " ${measurement.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Text(
                text = provenanceLabel(measurement.provenance),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Formats a measurement without inventing precision.
 *
 * Trailing zeros are trimmed rather than padding the value out to a fixed
 * number of decimal places, and integers are shown without a decimal point, so
 * `230.0 V` never appears as `230.00 V`.
 */
internal fun formatMeasurement(value: Double): String = when {
    value == value.toLong().toDouble() -> value.toLong().toString()
    else -> {
        val rounded = Math.round(value * 100.0) / 100.0
        if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }
}

private fun provenanceLabel(provenance: Measurement.Provenance): String = when (provenance) {
    Measurement.Provenance.MeterReported -> "Read from meter"
    Measurement.Provenance.Derived -> "Calculated from meter data"
}

/**
 * A tappable row representing a selectable entity, e.g. a meter profile.
 *
 * Uses [androidx.compose.foundation.selection.selectable] so that a single
 * radio control is announced once for the whole row rather than as a separate
 * focusable element, and so the whole row is one touch target.
 */
@Composable
fun SelectableRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.sm))
            .selectable(
                selected = selected,
                onClick = onSelect,
            )
            .padding(vertical = Spacing.md, horizontal = Spacing.sm)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(title)
                    if (subtitle != null) append(", $subtitle")
                    append(if (selected) ", selected" else "")
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * A tappable row that navigates somewhere.
 *
 * The chevron is decorative and hidden from accessibility; the row's own label
 * already conveys that it is actionable.
 */
@Composable
fun NavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.sm))
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.md, horizontal = Spacing.sm)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        leading?.invoke()
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * A small circular marker used to carry a status inside a dense row.
 */
@Composable
fun StatusBadge(statusLabel: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(ComponentSize.minTouchTarget / 2)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .semantics { contentDescription = statusLabel },
    )
}

/**
 * A responsive grid of measurement tiles.
 *
 * Uses `GridCells.Adaptive` so tiles keep a readable minimum width: on a 360dp
 * phone this yields a single column, on a tablet two or three, without a
 * width breakpoint anywhere in the code.
 */
@Composable
fun MeasurementGrid(
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.grid.LazyGridScope.() -> Unit,
) {
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(minSize = 160.dp),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        content = content,
    )
}
