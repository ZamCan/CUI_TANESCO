package com.tanesco.metercompanion.app.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tanesco.metercompanion.app.designsystem.theme.ComponentSize
import com.tanesco.metercompanion.app.designsystem.theme.Radii
import com.tanesco.metercompanion.app.designsystem.theme.Spacing
import com.tanesco.metercompanion.app.designsystem.theme.TanescoTheme

/**
 * One physical key on the meter keypad.
 *
 * Keys keep a fixed height rather than a fixed aspect ratio: a keypad whose keys
 * change shape with the number of columns would break the alignment a user
 * relies on when reaching for a key without looking.
 */
@Composable
private fun MeterKey(
    label: String,
    accessibilityLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    container: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    fontSize: TextUnit = 20.sp,
) {
    val shape = RoundedCornerShape(Radii.sm)
    Box(
        modifier = modifier
            .height(ComponentSize.buttonHeight)
            .clip(shape)
            .background(container)
            .border(
                width = 1.dp,
                color = if (enabled) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = accessibilityLabel },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The keypad of a prepaid meter.
 *
 * Laid out exactly as the hardware, because muscle memory is the entire point:
 *
 * ```
 *   7  8  9   ▲
 *   4  5  6   ▼
 *   1  2  3
 *   CANCEL  0  ENTER
 * ```
 *
 * Digits fill the first three columns and the page arrows sit in a fourth, which
 * is the arrangement used by this class of meter. CANCEL is bottom-left and ENTER
 * bottom-right, and they are the only two differently coloured keys - which is
 * what lets a user find them without reading the labels.
 *
 * The arrows page the display and are inert when the entry fits on one page,
 * matching the hardware rather than offering controls that do nothing.
 */
@Composable
fun MeterKeypad(
    onDigit: (Char) -> Unit,
    onCancel: () -> Unit,
    onEnter: () -> Unit,
    onPageNext: () -> Unit,
    onPagePrevious: () -> Unit,
    modifier: Modifier = Modifier,
    enterEnabled: Boolean = true,
    cancelEnabled: Boolean = true,
    pagingEnabled: Boolean = false,
) {
    val status = TanescoTheme.status

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            DigitKey("7", onDigit)
            DigitKey("8", onDigit)
            DigitKey("9", onDigit)
            ArrowKey("▲", "Show earlier digits", onPagePrevious, pagingEnabled)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            DigitKey("4", onDigit)
            DigitKey("5", onDigit)
            DigitKey("6", onDigit)
            ArrowKey("▼", "Show later digits", onPageNext, pagingEnabled)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            DigitKey("1", onDigit)
            DigitKey("2", onDigit)
            DigitKey("3", onDigit)
            // Keeps the arrow column aligned on the row below it.
            Box(Modifier.height(ComponentSize.buttonHeight))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            MeterKey(
                label = "CANCEL",
                accessibilityLabel = "Cancel entry",
                onClick = onCancel,
                modifier = Modifier.weight(2f),
                enabled = cancelEnabled,
                container = status.problemContainer,
                contentColor = status.onProblemContainer,
                fontSize = 12.sp,
            )
            DigitKey("0", onDigit)
            MeterKey(
                label = "ENTER",
                accessibilityLabel = "Record token",
                onClick = onEnter,
                modifier = Modifier.weight(2f),
                enabled = enterEnabled,
                container = status.readyContainer,
                contentColor = status.onReadyContainer,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun RowScope.DigitKey(digit: String, onDigit: (Char) -> Unit) {
    MeterKey(
        label = digit,
        accessibilityLabel = "Digit $digit",
        onClick = { onDigit(digit[0]) },
        modifier = Modifier.weight(1f),
        fontSize = 20.sp,
    )
}

@Composable
private fun RowScope.ArrowKey(
    symbol: String,
    accessibilityLabel: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    MeterKey(
        label = symbol,
        accessibilityLabel = accessibilityLabel,
        onClick = onClick,
        modifier = Modifier.weight(1f),
        enabled = enabled,
        fontSize = 14.sp,
    )
}

/**
 * The recessed window around the LCD.
 *
 * Real meters sit behind a green-grey polariser. Reproducing the inset keeps the
 * display from reading as a flat text field pasted onto a card.
 */
@Composable
fun LcdBezel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val polariser = if (isSystemInDarkTheme()) Color(0xFF2C3326) else Color(0xFFC9D2BE)
    val shape = RoundedCornerShape(Radii.xs)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(polariser)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * The counter and page indicator printed under the meter's display.
 *
 * "20 / 20" is what tells a user the entry is complete before pressing ENTER,
 * which is the same feedback the hardware gives.
 */
@Composable
fun MeterDisplayFooter(
    entryCount: Int,
    capacity: Int,
    page: Int,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$entryCount / $capacity",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (pageCount > 1) {
            Text(
                text = "page ${page + 1} of $pageCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
