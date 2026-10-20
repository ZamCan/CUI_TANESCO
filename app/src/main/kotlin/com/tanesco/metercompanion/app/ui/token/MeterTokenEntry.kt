package com.tanesco.metercompanion.app.ui.token

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tanesco.metercompanion.app.designsystem.component.LcdBezel
import com.tanesco.metercompanion.app.designsystem.component.MeterDisplayFooter
import com.tanesco.metercompanion.app.designsystem.component.MeterIdentityStrip
import com.tanesco.metercompanion.app.designsystem.component.MeterIndicatorRow
import com.tanesco.metercompanion.app.designsystem.component.MeterKeypad
import com.tanesco.metercompanion.app.designsystem.component.NoticePanel
import com.tanesco.metercompanion.app.designsystem.component.PanelDivider
import com.tanesco.metercompanion.app.designsystem.component.SectionHeader
import com.tanesco.metercompanion.app.designsystem.component.SegmentDisplay
import com.tanesco.metercompanion.app.designsystem.component.standardMeterIndicators
import com.tanesco.metercompanion.app.designsystem.theme.Spacing
import com.tanesco.metercompanion.core.token.MeterKeypadInput

/**
 * The token entry stage, presented as the meter's own front panel.
 *
 * Everything on this panel - the indicator order, the display window, the keypad
 * layout, the colours of CANCEL and ENTER - follows the physical meter, so a
 * customer can enter a token with the same hand movements they use at the meter.
 *
 * ## What this panel is not
 *
 * It is an *input surface*, not a device. Two consequences are enforced in the UI
 * rather than merely documented:
 *
 *  - the CONTACT indicator is never lit, because nothing here communicates with
 *    a meter, and its legend says so
 *  - ENTER records the entry locally and the confirmation says "Recorded locally"
 *
 * @param keypad the digit buffer; the single source of truth for the entry
 * @param failedCheckCount non-zero when the local structural checks are failing,
 *   which lights FAULT exactly as the meter lights it for a malformed entry
 */
@Composable
fun MeterTokenEntryPanel(
    keypad: MeterKeypadInput,
    failedCheckCount: Int,
    accountLabel: String,
    onDigit: (Char) -> Unit,
    onCancel: () -> Unit,
    onEnter: () -> Unit,
    onPageNext: () -> Unit,
    onPagePrevious: () -> Unit,
    modifier: Modifier = Modifier,
    isSubmitting: Boolean = false,
) {
    val hasEntry = !keypad.isEmpty
    val hasFailure = hasEntry && failedCheckCount > 0
    val statusLit = hasEntry && !hasFailure

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        MeterIdentityStrip(accountLabel = accountLabel)

        // The legend sits above the LEDs so the meaning of each indicator is
        // known before it is read, not after.
        MeterIndicatorRow(
            indicators = standardMeterIndicators(
                statusLit = statusLit,
                faultLit = hasFailure,
                // Always false. There is no meter to communicate with, and this
                // indicator must never imply otherwise.
                contactLit = false,
            ),
        )

        Spacer(Modifier.height(Spacing.xs))

        LcdBezel {
            Column {
                SegmentDisplay(
                    text = keypad.visiblePage.ifEmpty { idleDisplayFor(keypad) },
                    cells = keypad.digitsPerPage,
                )
                Spacer(Modifier.height(Spacing.xs))
                MeterDisplayFooter(
                    entryCount = keypad.digits.length,
                    capacity = keypad.maxDigits,
                    page = keypad.page,
                    pageCount = keypad.pageCount,
                )
            }
        }

        MeterKeypad(
            onDigit = onDigit,
            onCancel = onCancel,
            onEnter = onEnter,
            onPageNext = onPageNext,
            onPagePrevious = onPagePrevious,
            enterEnabled = hasEntry && !isSubmitting,
            cancelEnabled = hasEntry && !isSubmitting,
            pagingEnabled = keypad.pageCount > 1,
        )
    }
}

/**
 * What the display shows before anything has been entered.
 *
 * A real meter shows a row of dashes, which is a deliberate "no value" marker
 * rather than a zero - so it is reproduced exactly, and it never reads as a
 * measurement.
 */
private fun idleDisplayFor(keypad: MeterKeypadInput): String =
    "-".repeat(keypad.digitsPerPage)

/**
 * The explanation printed under the keypad.
 *
 * Stated once, in full, at the point of use. Everything else on the panel is
 * either self-evident or labelled.
 */
@Composable
fun MeterEntryExplanation(modifier: Modifier = Modifier) {
    NoticePanel(
        title = "This panel is a keypad, not a connection",
        body = "The layout, the indicators and the keys match your meter so that token " +
            "entry feels the same. Nothing here talks to a meter: no token is sent, and " +
            "the CONTACT indicator stays off. ENTER saves the entry on this device and " +
            "runs local format checks only.",
        modifier = modifier,
    )
}

/** Section heading above the entry panel. */
@Composable
fun EntrySectionHeader(modifier: Modifier = Modifier) {
    SectionHeader(title = "Token entry", modifier = modifier)
}

/** Padding shared by the entry column. */
val EntryPadding = PaddingValues(Spacing.md)

/** Row helper kept for future two-column layouts on wide screens. */
@Composable
fun EntryRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        content()
    }
}
