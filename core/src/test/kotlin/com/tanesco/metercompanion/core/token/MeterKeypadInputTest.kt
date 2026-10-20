package com.tanesco.metercompanion.core.token

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The meter keypad input model.
 *
 * These tests pin the hardware behaviours a customer already relies on: a fixed
 * capacity, CANCEL discarding the whole entry, and a long token being paged
 * rather than truncated.
 */
class MeterKeypadInputTest {

    private fun keypad(max: Int = 20, page: Int = 8) = MeterKeypadInput(maxDigits = max, digitsPerPage = page)

    private fun press(vararg digits: Char): MeterKeypadInput =
        digits.fold(keypad()) { state, digit -> state.pressDigit(digit) }

    @Test
    fun `digits are appended in order`() {
        assertEquals("1234", press('1', '2', '3', '4').digits)
    }

    @Test
    fun `non digits are ignored rather than rejected`() {
        // The hardware simply does not respond to a letter; it must not clear
        // the buffer or raise an error.
        var state = press('1', '2')
        state = state.pressDigit('a')
        state = state.pressDigit('-')
        state = state.pressDigit(' ')
        assertEquals("12", state.digits)
    }

    @Test
    fun `buffer stops at capacity and ignores further digits`() {
        val twenty = "12345678901234567890"
        val state = press(*twenty.toCharArray())
        assertEquals(20, state.digits.length)
        assertTrue(state.isFull)
        assertEquals(0, state.remaining)
        // A 21st press must be dropped, exactly as the hardware does.
        assertEquals(state.digits, state.pressDigit('9').digits)
    }

    @Test
    fun `capacity is configurable and enforced`() {
        val state = press('1', '2', '3', '4', '5')
        assertEquals(5, state.digits.length)
    }

    @Test
    fun `backspace removes the most recent digit`() {
        assertEquals("12", press('1', '2', '3').pressBackspace().digits)
    }

    @Test
    fun `backspace on an empty buffer changes nothing`() {
        val empty = keypad()
        assertEquals(empty.digits, empty.pressBackspace().digits)
    }

    @Test
    fun `cancel discards the entire entry`() {
        assertEquals("", press('1', '2', '3', '4').pressCancel().digits)
    }

    @Test
    fun `cancel returns the display to the first page`() {
        val paged = press('1', '2', '3', '4', '5', '6', '7', '8', '9').pressPageNext()
        assertEquals(1, paged.page)
        assertEquals(0, paged.pressCancel().page)
    }

    @Test
    fun `a twenty digit token spans three display pages`() {
        val state = press(*"12345678901234567890".toCharArray())
        assertEquals(3, state.pageCount)
        // The display follows the newest digit, as a meter's LCD does while a
        // token is keyed in, so the last group is the one shown.
        assertEquals("7890", state.visiblePage)
        // Paging back reveals the beginning of the token.
        assertEquals("12345678", state.pressPagePrevious().pressPagePrevious().visiblePage)
    }

    @Test
    fun `paging forward reveals later digits and clamps at the end`() {
        var state = press(*"12345678901234567890".toCharArray())
        // Typing left the display on the last page; step back to the middle.
        state = state.pressPagePrevious()
        assertEquals(1, state.page)
        assertEquals("90123456", state.visiblePage)

        state = state.pressPageNext()
        assertEquals("7890", state.visiblePage)
        // Further presses do nothing rather than wrapping or erroring.
        assertEquals(2, state.pressPageNext().page)
    }

    @Test
    fun `paging backward clamps at the first page`() {
        val state = press(*"12345678901234567890".toCharArray())
        assertEquals(
            0,
            state.pressPagePrevious().pressPagePrevious().pressPagePrevious().page,
        )
    }

    @Test
    fun `the display follows the newest digit`() {
        // Typing should not require paging to see what was just entered.
        var state = press(*"12345678".toCharArray())
        assertEquals(0, state.page)
        state = state.pressDigit('9')
        assertEquals(1, state.page)
        assertEquals("9", state.visiblePage)
    }

    @Test
    fun `an empty entry reports a blank display rather than zeros`() {
        val empty = keypad()
        assertTrue(empty.isEmpty)
        assertEquals("", empty.visiblePage)
        assertEquals(1, empty.pageCount)
    }

    @Test
    fun `grouped form matches the meter four digit grouping`() {
        assertEquals("1234 5678 9012 3456", press(*"1234567890123456".toCharArray()).grouped)
    }

    @Test
    fun `for profile uses the profile capacity`() {
        val keypad = MeterKeypadInput.forProfile(TokenFormatProfiles.Structural20Digit)
        assertEquals(20, keypad.maxDigits)
        assertEquals(TokenFormatProfiles.Structural20Digit.displayGroupSize, 4)
    }

    @Test
    fun `typed input is normalised before entering the buffer`() {
        // Pasted tokens arrive with spaces or dashes; they must fill the buffer
        // identically to keypad entry.
        val normalised = TokenNormalizer.normalize("1234 5678-9012 3456")
        val state = MeterKeypadInput(maxDigits = 20, digits = normalised)
        assertEquals("1234567890123456", state.digits)
        assertFalse(state.isFull)
    }
}
