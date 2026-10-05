package com.tanesco.metercompanion.app.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * The type scale.
 *
 * Built on the platform sans-serif rather than a bundled font file. A custom
 * typeface would add roughly half a megabyte to the APK for no functional gain,
 * and the platform font is what users already read every day on their device.
 *
 * Two deliberate choices:
 *
 *  - Tighter letter spacing on large text. Display sizes with default tracking
 *    read as marketing; this application reads as instrumentation.
 *  - A monospace style for identifiers (account numbers, tokens, codes).
 *    Meter and token numbers are transcribed by hand from a physical label;
 *    monospace makes digit-by-digit comparison possible and materially reduces
 *    transcription errors.
 */
internal val TanescoTypography: Typography = with(Typography()) {

    // Display and headline: used only for screen titles and the largest figures.
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
    )

    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp,
    )

    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    )

    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.1).sp,
    )

    // Title: card and panel headings.
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp,
    )

    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1.sp,
    )

    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    )

    // Body.
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp,
    )

    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.15.sp,
    )

    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.2.sp,
    )

    // Labels: buttons, chips, captions above fields.
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp,
    )

    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    )

    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp,
    )
}

/**
 * Style for values that are transcribed by hand: account numbers, tokens,
 * adapter identifiers and error codes.
 *
 * Monospace with generous tracking so that groups of digits stay aligned.
 */
val IdentifierTextStyle: TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
    letterSpacing = 0.6.sp,
)

/** Larger monospace style for a token shown on its own, e.g. in Token Center. */
val TokenValueTextStyle: TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 17.sp,
    lineHeight = 26.sp,
    letterSpacing = 1.sp,
    textAlign = TextAlign.Start,
)

/** Style for the large measurement figures on the Meter screen. */
val MeasurementValueTextStyle: TextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.4).sp,
)
