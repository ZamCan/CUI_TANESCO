package com.tanesco.metercompanion.app.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tanesco.metercompanion.app.designsystem.theme.Radii
import com.tanesco.metercompanion.app.designsystem.theme.Spacing
import com.tanesco.metercompanion.app.designsystem.theme.TanescoTheme
import kotlin.math.min

/**
 * A seven-segment LCD, drawn rather than typeset.
 *
 * A meter's display is a seven-segment part: it can only show glyphs from the
 * segment alphabet, and unlit segments remain faintly visible as "ghosts". Both
 * facts are reproduced here, because they are what make this read as a physical
 * instrument rather than a styled text box.
 *
 * Every cell draws all seven segments - lit ones in [onColor], unlit ones in
 * [offColor] - which is what produces the authentic ghosting.
 *
 * Characters outside the segment alphabet render as a blank cell rather than a
 * guessed glyph, so the display never invents something the hardware could not
 * show.
 */
@Composable
fun SegmentDisplay(
    text: String,
    modifier: Modifier = Modifier,
    /** Total character cells, padding the text with blanks. */
    cells: Int = text.length.coerceAtLeast(1),
    onColor: Color = SegmentOnDark,
    offColor: Color = SegmentOffDark,
    /** Describes the value to assistive technology. */
    accessibilityLabel: String = "Display showing ${text.ifBlank { "nothing" }}",
) {
    val padded = text.padEnd(cells.coerceAtLeast(1), ' ')

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SegmentDisplayHeight)
            .semantics { contentDescription = accessibilityLabel },
    ) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(SegmentDisplayHeight)
                // The parent Box already carries the accessible description.
                .clearAndSetSemantics { },
        ) {
            drawSegmentDisplay(padded, onColor, offColor)
        }
    }
}

/** Height of the display window. */
val SegmentDisplayHeight: Dp = 46.dp

/** Segment colours for the dark LCD used in this class of meter. */
val SegmentOnDark: Color = Color(0xFF16160F)
val SegmentOffDark: Color = Color(0x1416160F)

/**
 * Segment identifiers.
 *
 * `a` is the top bar, `g` the middle bar, `d` the bottom bar, `b`/`c` the
 * right verticals and `e`/`f` the left ones - the conventional lettering, used
 * here so the glyph table stays readable.
 */
private const val SEG_A = 'a'
private const val SEG_B = 'b'
private const val SEG_C = 'c'
private const val SEG_D = 'd'
private const val SEG_E = 'e'
private const val SEG_F = 'f'
private const val SEG_G = 'g'

/**
 * The seven-segment alphabet: each character mapped to its lit segments.
 */
private val SEGMENT_ALPHABET: Map<Char, String> = mapOf(
    '0' to "abcdef",
    '1' to "bc",
    '2' to "abdeg",
    '3' to "abcdg",
    '4' to "bcfg",
    '5' to "acdfg",
    '6' to "acdefg",
    '7' to "abc",
    '8' to "abcdefg",
    '9' to "abcdfg",
    '-' to "g",
)

private fun DrawScope.drawSegmentDisplay(text: String, onColor: Color, offColor: Color) {
    val cellCount = text.length.coerceAtLeast(1)
    val cellWidth = size.width / cellCount
    val stroke = (min(cellWidth, size.height) * 0.11f).coerceAtLeast(1.5f)
    val inset = stroke * 0.85f

    text.forEachIndexed { index, character ->
        val lit = SEGMENT_ALPHABET[character].orEmpty()

        val left = index * cellWidth
        val top = inset
        val bottom = size.height - inset
        val midY = (top + bottom) / 2f
        val centerX = left + cellWidth / 2f
        val halfLength = cellWidth / 2f - inset

        // Horizontal bars.
        drawSegmentBar(SEG_A, lit, horizontal = true, center = Offset(centerX, top),
            length = halfLength, thickness = stroke, onColor = onColor, offColor = offColor)
        drawSegmentBar(SEG_G, lit, horizontal = true, center = Offset(centerX, midY),
            length = halfLength, thickness = stroke, onColor = onColor, offColor = offColor)
        drawSegmentBar(SEG_D, lit, horizontal = true, center = Offset(centerX, bottom),
            length = halfLength, thickness = stroke, onColor = onColor, offColor = offColor)

        // Vertical bars.
        drawSegmentBar(SEG_F, lit, horizontal = false, center = Offset(left + inset, (top + midY) / 2f),
            length = (midY - top) / 2f, thickness = stroke, onColor = onColor, offColor = offColor)
        drawSegmentBar(SEG_E, lit, horizontal = false, center = Offset(left + inset, (midY + bottom) / 2f),
            length = (bottom - midY) / 2f, thickness = stroke, onColor = onColor, offColor = offColor)
        drawSegmentBar(SEG_B, lit, horizontal = false, center = Offset(left + cellWidth - inset, (top + midY) / 2f),
            length = (midY - top) / 2f, thickness = stroke, onColor = onColor, offColor = offColor)
        drawSegmentBar(SEG_C, lit, horizontal = false, center = Offset(left + cellWidth - inset, (midY + bottom) / 2f),
            length = (bottom - midY) / 2f, thickness = stroke, onColor = onColor, offColor = offColor)
    }
}

private fun DrawScope.drawSegmentBar(
    segment: Char,
    litSegments: String,
    horizontal: Boolean,
    center: Offset,
    length: Float,
    thickness: Float,
    onColor: Color,
    offColor: Color,
) {
    val color = if (litSegments.contains(segment)) onColor else offColor
    // The gap between segments is what makes a seven-segment display legible as
    // such; a continuous bar would read as a plain rectangle.
    val barLength = (length - thickness).coerceAtLeast(1f)
    val topLeft = if (horizontal) {
        Offset(center.x - barLength / 2f, center.y - thickness / 2f)
    } else {
        Offset(center.x - thickness / 2f, center.y - barLength / 2f)
    }
    val barSize = if (horizontal) Size(barLength, thickness) else Size(thickness, barLength)
    drawRoundRect(
        color = color,
        topLeft = topLeft,
        size = barSize,
        cornerRadius = CornerRadius(thickness / 2f),
    )
}

/**
 * One indicator LED and its printed label.
 */
data class MeterIndicator(
    val label: String,
    val lit: Boolean,
    val accent: Color,
    /** Plain-language explanation of when this indicator lights. */
    val meaning: String,
)

/**
 * The meter's indicator LEDs, left to right: STATUS, FAULT, CONTACT.
 *
 * Each is annotated with when it lights, because an indicator a user cannot
 * interpret is worse than no indicator at all. The layout matches the meter so
 * that the meaning transfers without explanation.
 */
@Composable
fun MeterIndicatorRow(
    indicators: List<MeterIndicator>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        indicators.forEach { indicator ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${indicator.label} indicator: " +
                            if (indicator.lit) "lit" else "off"
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(
                            color = if (indicator.lit) indicator.accent else Color.Transparent,
                            shape = RoundedCornerShape(Radii.pill),
                        )
                        .border(
                            width = 1.5.dp,
                            color = indicator.accent,
                            shape = RoundedCornerShape(Radii.pill),
                        ),
                )
                Text(
                    text = indicator.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The three standard meter indicators, with the application's semantics. */
@Composable
fun standardMeterIndicators(
    statusLit: Boolean,
    faultLit: Boolean,
    contactLit: Boolean,
): List<MeterIndicator> {
    val status = TanescoTheme.status
    return listOf(
        MeterIndicator(
            label = "STATUS",
            lit = statusLit,
            accent = status.ready,
            meaning = "A token has been entered and passes the local format checks.",
        ),
        MeterIndicator(
            label = "FAULT",
            lit = faultLit,
            accent = status.problem,
            meaning = "The entry does not match the expected format.",
        ),
        MeterIndicator(
            label = "CONTACT",
            lit = contactLit,
            accent = status.attention,
            meaning = "Lit only while the application is exchanging data with a meter. " +
                "It stays off in this build, because no meter communication is implemented.",
        ),
    )
}

/**
 * The meter's identity strip.
 *
 * States explicitly that this is a modelled keypad, not the meter: the panel is
 * an input surface, and saying so here prevents the whole screen from implying
 * that a physical device is present.
 */
@Composable
fun MeterIdentityStrip(
    accountLabel: String,
    modifier: Modifier = Modifier,
    caption: String = "Keypad modelled on your meter",
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = "PREPAID METER",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = accountLabel,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Hairline used inside the device panel to separate zones. */
@Composable
fun PanelDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}

/** Horizontal gap helper for keypad rows. */
@Composable
fun KeyGap(size: Dp = Spacing.sm) {
    Box(Modifier.width(size))
}
