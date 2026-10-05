package com.tanesco.metercompanion.core.token

/**
 * The digit-entry behaviour of a physical prepaid meter keypad.
 *
 * Prepaid electricity meters are used outdoors, by hand, at night, in the rain.
 * Their keypads are deliberately dumb and their input model is unforgiving:
 *
 *  - only digits are accepted; letters are ignored rather than corrected
 *  - the buffer has a fixed capacity, so a digit pressed at capacity is dropped
 *  - CANCEL discards the whole entry rather than deleting one digit
 *  - the display shows a fixed number of characters, so a 20-digit token is
 *    *paged* through with the arrow keys
 *
 * Reproducing that model exactly is the point: a customer who already knows
 * their meter does not have to learn a second one. It is also plain, immutable
 * logic with no UI dependency, so it is covered directly by unit tests.
 *
 * ## What this does not do
 *
 * Pressing ENTER here does not talk to a meter. It hands the digits to
 * [TokenService], which records them locally. The keypad is an input
 * *metaphor*, never a claim that a meter was reached.
 */
class MeterKeypadInput(
    /** Buffer capacity; taken from the active token format profile. */
    val maxDigits: Int,
    /** Characters the display shows at once, matching the meter's LCD. */
    val digitsPerPage: Int = DEFAULT_DIGITS_PER_PAGE,
    digits: String = "",
    page: Int = 0,
) {
    init {
        require(maxDigits > 0) { "maxDigits must be positive" }
        require(digitsPerPage > 0) { "digitsPerPage must be positive" }
        require(digits.all { it.isDigit() }) { "only digits may be buffered" }
    }

    /** The digits entered so far. */
    val digits: String = digits.take(maxDigits)

    /** The digit index currently shown on the display. */
    val page: Int = page.coerceIn(0, lastPage())

    val isEmpty: Boolean get() = digits.isEmpty()

    val isFull: Boolean get() = digits.length >= maxDigits

    /** Remaining capacity, so the UI can show "8 of 20". */
    val remaining: Int get() = (maxDigits - digits.length).coerceAtLeast(0)

    /** Number of display pages; always at least one so the UI has something to show. */
    val pageCount: Int
        get() = if (digits.isEmpty()) 1 else lastPage() + 1

    /** The slice of digits the display shows, grouped in fours like the meter. */
    val visiblePage: String
        get() {
            if (digits.isEmpty()) return BLANK_DISPLAY
            val start = page * digitsPerPage
            return digits.substring(start, minOf(start + digitsPerPage, digits.length))
        }

    /** The full entry, grouped in fours for the confirmation line. */
    val grouped: String
        get() = TokenNormalizer.formatForDisplay(digits, GROUP_SIZE)

    /**
     * Appends [digit].
     *
     * Non-digits and a full buffer are ignored, which is what the hardware does.
     * The display follows the newest digit so the customer can see what they
     * just typed without paging.
     */
    fun pressDigit(digit: Char): MeterKeypadInput {
        if (!digit.isDigit() || isFull) return this
        val next = digits + digit
        return copyWith(digits = next, page = pageForEndOf(next))
    }

    /** Removes the most recent digit. */
    fun pressBackspace(): MeterKeypadInput {
        if (digits.isEmpty()) return this
        val next = digits.dropLast(1)
        return copyWith(digits = next, page = pageForEndOf(next))
    }

    /** CANCEL: discards the entire entry. */
    fun pressCancel(): MeterKeypadInput = copyWith(digits = "", page = 0)

    /** Advances the display one page, clamped at the end. */
    fun pressPageNext(): MeterKeypadInput =
        if (page >= pageCount - 1) this else copyWith(page = page + 1)

    /** Moves the display back one page, clamped at the start. */
    fun pressPagePrevious(): MeterKeypadInput =
        if (page <= 0) this else copyWith(page = page - 1)

    /** True when [candidate] would fill the buffer exactly. */
    fun isCompleteAgainst(expectedLength: Int): Boolean = digits.length == expectedLength

    private fun lastPage(): Int = if (digits.isEmpty()) 0 else (digits.length - 1) / digitsPerPage

    private fun pageForEndOf(value: String): Int =
        if (value.isEmpty()) 0 else (value.length - 1) / digitsPerPage

    private fun copyWith(digits: String = this.digits, page: Int = this.page) =
        MeterKeypadInput(
            maxDigits = maxDigits,
            digitsPerPage = digitsPerPage,
            digits = digits,
            page = page,
        )

    companion object {
        /** Matches the 8-character window of a typical meter LCD. */
        const val DEFAULT_DIGITS_PER_PAGE = 8

        /** Meters group a 20-digit token into 4s. */
        const val GROUP_SIZE = 4

        /** What an empty meter display shows. */
        const val BLANK_DISPLAY = ""

        /**
         * Builds an input buffer sized for [profile].
         *
         * The capacity is the profile's maximum length, so a buffer can never
         * accept more digits than the rules allow.
         */
        fun forProfile(
            profile: TokenFormatProfile,
            digitsPerPage: Int = DEFAULT_DIGITS_PER_PAGE,
        ): MeterKeypadInput = MeterKeypadInput(
            maxDigits = profile.maxLength,
            digitsPerPage = digitsPerPage,
        )
    }
}
