package com.tanesco.metercompanion.app.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * The application palette.
 *
 * Design intent
 * -------------
 * This is infrastructure software, not a consumer gadget. The palette is
 * therefore built from warm neutral greys with a single, muted ochre accent
 * rather than the saturated blue-on-dark "technology" look, which reads as
 * decorative rather than professional.
 *
 * Rules that hold across the whole system:
 *
 *  - surfaces are separated by tone, not by heavy shadows or gradients
 *  - exactly one accent colour carries action; nothing else competes with it
 *  - semantic colours (ready / attention / problem) are reserved for meaning and
 *    are never used decoratively
 *
 * Every pairing below meets WCAG AA (4.5:1) for body text in both schemes.
 */
internal object TanescoPalette {

    // --- Light scheme ------------------------------------------------------
    val LightBackground = Color(0xFFFAF8F5)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceMuted = Color(0xFFF2EFEA)
    val LightSurfaceSunken = Color(0xFFEDE9E2)
    val LightOnSurface = Color(0xFF1B1917)
    val LightOnSurfaceMuted = Color(0xFF5A544C)
    val LightOutline = Color(0xFFD9D3C9)
    val LightOutlineStrong = Color(0xFFBCB4A8)

    /** Burnt ochre. White text on this is 5.6:1. */
    val LightPrimary = Color(0xFF8C4A21)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightPrimaryContainer = Color(0xFFF6E3D4)
    val LightOnPrimaryContainer = Color(0xFF34170A)

    // --- Dark scheme -------------------------------------------------------
    val DarkBackground = Color(0xFF14120F)
    val DarkSurface = Color(0xFF1C1A16)
    val DarkSurfaceMuted = Color(0xFF26231E)
    val DarkSurfaceSunken = Color(0xFF100E0C)
    val DarkOnSurface = Color(0xFFEDE9E2)
    val DarkOnSurfaceMuted = Color(0xFFB6AEA2)
    val DarkOutline = Color(0xFF3D3830)
    val DarkOutlineStrong = Color(0xFF565046)

    val DarkPrimary = Color(0xFFE9A05C)
    val DarkOnPrimary = Color(0xFF3A2008)
    val DarkPrimaryContainer = Color(0xFF5A3016)
    val DarkOnPrimaryContainer = Color(0xFFF8DCC3)

    // --- Semantic: ready / success ----------------------------------------
    val LightReady = Color(0xFF2F6B3A)
    val LightOnReady = Color(0xFFFFFFFF)
    val LightReadyContainer = Color(0xFFDDEEDD)
    val LightOnReadyContainer = Color(0xFF12290F)

    val DarkReady = Color(0xFF8FCB98)
    val DarkOnReady = Color(0xFF12300F)
    val DarkReadyContainer = Color(0xFF23401C)
    val DarkOnReadyContainer = Color(0xFFCDE9CF)

    // --- Semantic: attention / research ------------------------------------
    // Amber rather than yellow: readable on light surfaces without the
    // "warning tape" look that pure yellow produces.
    val LightAttention = Color(0xFF7A5200)
    val LightOnAttention = Color(0xFFFFFFFF)
    val LightAttentionContainer = Color(0xFFFBEBC8)
    val LightOnAttentionContainer = Color(0xFF2B1C00)

    val DarkAttention = Color(0xFFE0B65C)
    val DarkOnAttention = Color(0xFF2E2000)
    val DarkAttentionContainer = Color(0xFF453208)
    val DarkOnAttentionContainer = Color(0xFFF6E2B4)

    // --- Semantic: problem / error -----------------------------------------
    val LightProblem = Color(0xFF9E2A20)
    val LightOnProblem = Color(0xFFFFFFFF)
    val LightProblemContainer = Color(0xFFFBE0DD)
    val LightOnProblemContainer = Color(0xFF3B0A06)

    val DarkProblem = Color(0xFFF08A80)
    val DarkOnProblem = Color(0xFF3B0A06)
    val DarkProblemContainer = Color(0xFF4E1712)
    val DarkOnProblemContainer = Color(0xFFFBDAD6)

    // --- Semantic: neutral / unavailable -----------------------------------
    val LightNeutral = Color(0xFF5A544C)
    val LightOnNeutral = Color(0xFFFFFFFF)
    val LightNeutralContainer = Color(0xFFEAE5DD)
    val LightOnNeutralContainer = Color(0xFF2A251F)

    val DarkNeutral = Color(0xFFB6AEA2)
    val DarkOnNeutral = Color(0xFF2A251F)
    val DarkNeutralContainer = Color(0xFF38332C)
    val DarkOnNeutralContainer = Color(0xFFE2DCD2)

    // --- Information -------------------------------------------------------
    val LightInfo = Color(0xFF3F5A72)
    val LightInfoContainer = Color(0xFFDDE7F0)
    val LightOnInfoContainer = Color(0xFF12212E)

    val DarkInfo = Color(0xFF9CC0D8)
    val DarkInfoContainer = Color(0xFF27394A)
    val DarkOnInfoContainer = Color(0xFFD5E6F2)
}
