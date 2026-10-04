package com.tanesco.metercompanion.core.token

import com.tanesco.metercompanion.core.model.TokenFormatProfileIds

/**
 * The structural expectations applied to a token value.
 *
 * ## Why this is configurable
 *
 * Token layouts are specified per utility and per meter generation, and the
 * authoritative specification is not published. Rather than hard-coding a guess
 * and presenting it as fact, the application ships a clearly labelled
 * *structural* profile and keeps the shape of the rule set pluggable. See
 * docs/decisions/0006-token-format.md.
 */
data class TokenFormatProfile(
    val id: String,
    val label: String,
    /** Shown in Settings and in token validation results. */
    val description: String,
    val allowedCharacters: Set<Char>,
    val minLength: Int,
    val maxLength: Int,
    /** Block size used when grouping a token for display. */
    val displayGroupSize: Int,
    /**
     * Integrity check to apply, or `null` when no published specification
     * exists. Always `null` in the shipped profile.
     */
    val integrityCheck: TokenIntegrityCheck? = null,
) {
    init {
        require(allowedCharacters.isNotEmpty()) { "allowedCharacters must not be empty" }
        require(minLength > 0) { "minLength must be positive" }
        require(maxLength >= minLength) { "maxLength must be >= minLength" }
        require(displayGroupSize > 0) { "displayGroupSize must be positive" }
    }

    /** The rule pipeline implied by this profile. */
    fun buildRules(): List<TokenRule> = buildList {
        add(NotBlankTokenRule)
        add(CharacterSetTokenRule(allowedCharacters))
        add(LengthTokenRule(minLength, maxLength))
        if (allowedCharacters == DIGITS) add(UniformDigitTokenRule())
        integrityCheck?.let { check ->
            add(IntegrityTokenRule(check))
        }
    }

    companion object {
        val DIGITS: Set<Char> = ('0'..'9').toSet()
    }
}

/** Adapts a [TokenIntegrityCheck] to the [TokenRule] contract. */
class IntegrityTokenRule(
    private val check: TokenIntegrityCheck,
) : TokenRule {
    override val id: String = "token.integrity"
    override val label: String = check.label

    override fun check(normalized: String): TokenRuleVerdict =
        if (check.verify(normalized)) {
            TokenRuleVerdict.pass()
        } else {
            TokenRuleVerdict.error("This token failed its integrity check. Re-check the digits and try again.")
        }
}

/**
 * The set of token format profiles known to the application.
 *
 * Lookups fall back to [default] rather than throwing: a settings row written by
 * a newer build must never crash an older one.
 */
class TokenFormatProfileRegistry(
    profiles: List<TokenFormatProfile> = listOf(TokenFormatProfiles.Structural20Digit),
) {
    val profiles: List<TokenFormatProfile> = profiles.toList()

    val default: TokenFormatProfile = profiles.first()

    fun findById(id: String): TokenFormatProfile? = profiles.firstOrNull { it.id == id }

    /** Returns the matching profile, or [default] when [id] is unknown. */
    fun resolve(id: String): TokenFormatProfile = findById(id) ?: default
}

/**
 * Built-in profiles.
 */
object TokenFormatProfiles {

    /**
     * A 20-digit numeric profile.
     *
     * This is the shape most commonly seen on the token field of utility meters,
     * but it is applied here purely as a *structural* profile: it checks
     * presence, character set, length and obvious input mistakes, and nothing
     * else. Passing it means "this looks like a well-formed token", never
     * "this token is valid".
     */
    val Structural20Digit: TokenFormatProfile = TokenFormatProfile(
        id = TokenFormatProfileIds.Default,
        label = "20-digit numeric (structural)",
        description = "Checks length, digits and obvious input mistakes. " +
            "It does not confirm that a meter will accept the token.",
        allowedCharacters = TokenFormatProfile.DIGITS,
        minLength = 20,
        maxLength = 20,
        displayGroupSize = 4,
        integrityCheck = null,
    )
}
