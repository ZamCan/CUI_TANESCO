package com.tanesco.metercompanion.core.token

/**
 * Converts raw user input into the canonical form used for storage and
 * comparison.
 *
 * Normalisation removes *only* known separators. Characters outside the
 * expected set are deliberately preserved so that validation can report them,
 * instead of being silently swallowed.
 */
object TokenNormalizer {

    /**
     * Characters treated as presentation-only separators.
     *
     * Includes the non-breaking space (U+00A0) and narrow no-break space
     * (U+202F), both of which appear when tokens are copied from PDFs and web
     * pages and would otherwise look like legitimate input errors.
     */
    private val SEPARATORS: Set<Char> = setOf(
        ' ', '\t', '\n', '\r',
        '-', '_', '.', ',',
        '\u00A0', '\u2007', '\u202F',
    )

    /** Returns [raw] upper-cased with all separators removed. */
    fun normalize(raw: String): String {
        val builder = StringBuilder(raw.length)
        for (character in raw) {
            if (character in SEPARATORS) continue
            builder.append(character.uppercaseChar())
        }
        return builder.toString()
    }

    /** True when [raw] contains nothing but separators. */
    fun isBlank(raw: String): Boolean = normalize(raw).isEmpty()

    /**
     * Groups [normalized] into fixed size blocks for readability.
     *
     * Used for display only. The stored canonical value is never modified.
     */
    fun formatForDisplay(normalized: String, groupSize: Int = DEFAULT_GROUP_SIZE): String {
        require(groupSize > 0) { "groupSize must be positive" }
        if (normalized.isEmpty()) return normalized
        return normalized.chunked(groupSize).joinToString(GROUP_SEPARATOR)
    }

    const val DEFAULT_GROUP_SIZE = 4
    private const val GROUP_SEPARATOR = " "
}
