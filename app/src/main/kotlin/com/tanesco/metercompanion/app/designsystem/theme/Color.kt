package com.tanesco.metercompanion.app.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * The application palette — "TANESCO Glass".
 *
 * Colour relationships (intentional, taken from the real TANESCO identity):
 *
 *  - The real TANESCO mark is a deep green + sun-yellow swoosh pair around a
 *    lightning bolt. This palette is built from exactly those two hues:
 *    TANESCO green carries action and identity; sun-yellow is a highlight
 *    reserved for brand moments (logo, aurora wash, selected-navigation ring).
 *  - Green and yellow are warm-adjacent, so a cool deep-teal ink is used as
 *    the night base and a soft mint as the light base — this keeps the brand
 *    pair luminous instead of muddy at glass translucency.
 *  - Red and amber keep ONLY their semantic meanings (problem / attention).
 *    Yellow-brand is never used for a warning, and amber-attention is a
 *    deeper burnt orange so the two can never be confused.
 *  - Body-text pairings are chosen to meet WCAG AA on their intended glass
 *    containers; translucent fills always sit over the theme background.
 */

/** Raw brand hues, sampled from and derived from the official TANESCO logo. */
object TanescoBrand {
    val DeepGreen   = Color(0xFF0B4D20)   // TANESCO green, darkened for text/action
    val TanescoGreen = Color(0xFF177A2E)  // logo swoosh green
    val EnergyGreen = Color(0xFF2DBC5E)   // luminous green for dark glass
    val SunYellow   = Color(0xFFFFCD08)   // logo swoosh yellow (brand highlight only)
    val GoldAmber   = Color(0xFFF5A623)   // gradient partner for primary actions
    val InkTeal     = Color(0xFF04150B)   // night-observatory base
    val MintMist    = Color(0xFFF0F7EC)   // daylight glass base
}

internal object TanescoPalette {

    // --- Light scheme: daylight glass -------------------------------------
    val LightBackground = Color(0xFFEEF6EA)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceMuted = Color(0xFFE4F1DC)
    val LightSurfaceSunken = Color(0xFFD6E8CC)
    val LightOnSurface = Color(0xFF0C2313)
    val LightOnSurfaceMuted = Color(0xFF43584A)
    val LightOutline = Color(0xFFBFD4B6)
    val LightOutlineStrong = Color(0xFF8FAE87)

    /** TANESCO green. White text on this is ~5.9:1. */
    val LightPrimary = Color(0xFF146C2E)
    val LightOnPrimary = Color(0xFFFFFFFF)
    val LightPrimaryContainer = Color(0xFFD3F0D8)
    val LightOnPrimaryContainer = Color(0xFF062A12)

    // --- Dark scheme: emerald-ink glass ------------------------------------
    val DarkBackground = Color(0xFF04150B)
    val DarkSurface = Color(0xFF0B2416)
    val DarkSurfaceMuted = Color(0xFF123420)
    val DarkSurfaceSunken = Color(0xFF020B05)
    val DarkOnSurface = Color(0xFFEAF6E9)
    val DarkOnSurfaceMuted = Color(0xFFA9C3AC)
    val DarkOutline = Color(0xFF274A31)
    val DarkOutlineStrong = Color(0xFF3D6B4B)

    val DarkPrimary = Color(0xFF7DEFA0)
    val DarkOnPrimary = Color(0xFF04240F)
    val DarkPrimaryContainer = Color(0xFF14532D)
    val DarkOnPrimaryContainer = Color(0xFFD8F7DE)

    // --- Semantic: ready / success -----------------------------------------
    val LightReady = Color(0xFF177245)
    val LightOnReady = Color(0xFFFFFFFF)
    val LightReadyContainer = Color(0xFFD9F2E2)
    val LightOnReadyContainer = Color(0xFF07301A)

    val DarkReady = Color(0xFF86E3A9)
    val DarkOnReady = Color(0xFF052A15)
    val DarkReadyContainer = Color(0xFF16452A)
    val DarkOnReadyContainer = Color(0xFFD5F5DF)

    // --- Semantic: attention / research -------------------------------------
    // Burnt orange-amber, deliberately NOT the brand sun-yellow.
    val LightAttention = Color(0xFF9A5900)
    val LightOnAttention = Color(0xFFFFFFFF)
    val LightAttentionContainer = Color(0xFFFEEDC9)
    val LightOnAttentionContainer = Color(0xFF341C00)

    val DarkAttention = Color(0xFFFFC266)
    val DarkOnAttention = Color(0xFF3A2100)
    val DarkAttentionContainer = Color(0xFF54360A)
    val DarkOnAttentionContainer = Color(0xFFFFE7BC)

    // --- Semantic: problem / error ------------------------------------------
    val LightProblem = Color(0xFFB3261E)
    val LightOnProblem = Color(0xFFFFFFFF)
    val LightProblemContainer = Color(0xFFFBE3E0)
    val LightOnProblemContainer = Color(0xFF400A06)

    val DarkProblem = Color(0xFFFF9A8F)
    val DarkOnProblem = Color(0xFF400A06)
    val DarkProblemContainer = Color(0xFF5A1A14)
    val DarkOnProblemContainer = Color(0xFFFFDCD7)

    // --- Semantic: neutral / unavailable ------------------------------------
    val LightNeutral = Color(0xFF4A5A4F)
    val LightOnNeutral = Color(0xFFFFFFFF)
    val LightNeutralContainer = Color(0xFFE2EADF)
    val LightOnNeutralContainer = Color(0xFF16241A)

    val DarkNeutral = Color(0xFFB9CCBA)
    val DarkOnNeutral = Color(0xFF16241A)
    val DarkNeutralContainer = Color(0xFF24382A)
    val DarkOnNeutralContainer = Color(0xFFE3EFE2)

    // --- Information ---------------------------------------------------------
    val LightInfo = Color(0xFF166A8A)
    val LightInfoContainer = Color(0xFFD8EFF7)
    val LightOnInfoContainer = Color(0xFF042633)

    val DarkInfo = Color(0xFF8FD6F2)
    val DarkInfoContainer = Color(0xFF17445A)
    val DarkOnInfoContainer = Color(0xFFD8F2FC)
}
