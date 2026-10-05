package com.tanesco.metercompanion.core.token

import com.tanesco.metercompanion.core.model.TokenCheckSeverity

/**
 * One independently testable condition a token value must satisfy.
 *
 * Rules are pure functions of the normalised value. Anything needing I/O - such
 * as duplicate detection - is deliberately *not* a rule; it belongs to the
 * orchestration layer so that rules stay trivially unit testable.
 */
interface TokenRule {

    /** Stable identifier, used in reports and tests. */
    val id: String

    /** Short human readable name for the check. */
    val label: String

    /** Evaluates [normalized] and returns a verdict. */
    fun check(normalized: String): TokenRuleVerdict
}

/** Outcome of a single [TokenRule]. */
data class TokenRuleVerdict(
    val passed: Boolean,
    val severity: TokenCheckSeverity,
    /** User-facing explanation. Written to be shown verbatim. */
    val message: String,
) {
    companion object {
        fun pass(message: String = "OK"): TokenRuleVerdict =
            TokenRuleVerdict(true, TokenCheckSeverity.Error, message)

        fun error(message: String): TokenRuleVerdict =
            TokenRuleVerdict(false, TokenCheckSeverity.Error, message)

        fun warning(message: String): TokenRuleVerdict =
            TokenRuleVerdict(false, TokenCheckSeverity.Warning, message)
    }
}

/** Rejects an empty value. Always the first rule in a pipeline. */
object NotBlankTokenRule : TokenRule {
    override val id: String = "token.not_blank"
    override val label: String = "Token is present"

    override fun check(normalized: String): TokenRuleVerdict =
        if (normalized.isEmpty()) {
            TokenRuleVerdict.error("Enter a token value.")
        } else {
            TokenRuleVerdict.pass()
        }
}

/**
 * Enforces the permitted character set.
 *
 * [unexpected] is computed so the message can name the offending character,
 * which turns an opaque "invalid token" into an actionable message.
 */
class CharacterSetTokenRule(
    private val allowed: Set<Char>,
) : TokenRule {

    override val id: String = "token.character_set"
    override val label: String = "Uses only permitted characters"

    override fun check(normalized: String): TokenRuleVerdict {
        val unexpected = normalized.filterNot { it in allowed }.toList()
        return if (unexpected.isEmpty()) {
            TokenRuleVerdict.pass()
        } else {
            val shown = unexpected.distinct().take(4).joinToString(", ") { "'$it'" }
            TokenRuleVerdict.error(
                "Remove the following character${if (unexpected.size == 1) "" else "s"}: $shown.",
            )
        }
    }
}

/** Enforces the accepted length range. */
class LengthTokenRule(
    private val minLength: Int,
    private val maxLength: Int,
) : TokenRule {

    init {
        require(minLength > 0) { "minLength must be positive" }
        require(maxLength >= minLength) { "maxLength must be >= minLength" }
    }

    override val id: String = "token.length"
    override val label: String = "Has an accepted length"

    override fun check(normalized: String): TokenRuleVerdict {
        val length = normalized.length
        return when {
            length < minLength -> TokenRuleVerdict.error(
                "A token is at least $minLength characters; this one has $length.",
            )

            length > maxLength -> TokenRuleVerdict.error(
                "A token is at most $maxLength characters; this one has $length.",
            )

            else -> TokenRuleVerdict.pass()
        }
    }
}

/**
 * Rejects values made of a single repeated character.
 *
 * These pass every other structural rule while being obvious input mistakes,
 * and they are the most common real-world fat-finger on a numeric field. It is
 * a structural sanity check only and makes no claim about validity.
 */
class UniformDigitTokenRule : TokenRule {

    override val id: String = "token.not_uniform"
    override val label: String = "Is not a repeated character"

    override fun check(normalized: String): TokenRuleVerdict {
        if (normalized.length < 2) return TokenRuleVerdict.pass()
        val first = normalized.first()
        val repeated = normalized.all { it == first }
        return if (repeated) {
            TokenRuleVerdict.error("A token cannot be the same character repeated ${normalized.length} times.")
        } else {
            TokenRuleVerdict.pass()
        }
    }
}

/**
 * Optional integrity check port.
 *
 * No implementation ships with the application. A checksum can only be applied
 * once a specification is available from an authoritative source; inventing one
 * would produce tokens that look verified but are not. The port exists so that
 * adding a real check later is a small, local change.
 */
interface TokenIntegrityCheck {

    val id: String

    val label: String

    fun verify(normalized: String): Boolean
}
