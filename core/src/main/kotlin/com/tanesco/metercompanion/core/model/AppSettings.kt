package com.tanesco.metercompanion.core.model

/**
 * User-controlled application settings.
 *
 * Persisted as a single row so that reading settings is one query. Defaults are
 * defined by [AppSettings.Default] and are what a fresh install sees.
 */
data class AppSettings(
    /** Profile selected as "the" meter for Home and Meter screens. */
    val defaultMeterId: String?,
    val appearance: AppearanceMode,
    val unitSystem: UnitSystem,
    val language: AppLanguage,
    /** Which structural rules the token engine applies. */
    val tokenFormatProfileId: String,
    /**
     * Allows the user to request a connection attempt.
     *
     * Defaults to `false`. Even when enabled it can only ever produce an
     * honest "unavailable" result, but keeping it off by default means the
     * default experience never implies a meter was reachable.
     */
    val allowConnectionAttempts: Boolean,
    /** Surfaces additional technical detail in Diagnostics and About. */
    val developerMode: Boolean,
) {
    companion object {
        val Default: AppSettings = AppSettings(
            defaultMeterId = null,
            appearance = AppearanceMode.System,
            unitSystem = UnitSystem.Metric,
            language = AppLanguage.System,
            tokenFormatProfileId = TokenFormatProfileIds.Default,
            allowConnectionAttempts = false,
            developerMode = false,
        )
    }
}

enum class AppearanceMode(val label: String) {
    System("Match system"),
    Light("Light"),
    Dark("Dark"),
}

/**
 * Unit system used when presenting energy values.
 *
 * Only reachable values will ever be formatted, so this currently affects how
 * meter-reported energy would be displayed and nothing else.
 */
enum class UnitSystem(val label: String) {
    Metric("Metric (kWh)"),
    Imperial("Imperial (kBTU)"),
}

/**
 * Language selection.
 *
 * Only locales that ship a reviewed translation are offered. See
 * docs/decisions/0007-localisation.md for why machine translations are not
 * bundled.
 */
enum class AppLanguage(val label: String) {
    System("Match system"),
    English("English"),
}

/** Stable identifiers of the token format profiles shipped with the application. */
object TokenFormatProfileIds {
    /**
     * Structural profile used until a per-utility specification is available.
     * See docs/decisions/0006-token-format.md.
     */
    const val Default = "structural-20-digit-v1"
}
