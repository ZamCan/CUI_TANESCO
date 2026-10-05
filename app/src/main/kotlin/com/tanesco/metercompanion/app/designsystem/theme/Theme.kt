package com.tanesco.metercompanion.app.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.tanesco.metercompanion.core.model.AppearanceMode

/**
 * Semantic colours that Material's [androidx.compose.material3.ColorScheme] has
 * no slot for.
 *
 * Status colours are not an extension of the brand palette; they carry meaning,
 * so they are kept separate. That separation is what makes it impossible to
 * accidentally render an "error" chip in the brand accent colour.
 */
data class StatusColors(
    val ready: Color,
    val onReady: Color,
    val readyContainer: Color,
    val onReadyContainer: Color,
    val attention: Color,
    val onAttention: Color,
    val attentionContainer: Color,
    val onAttentionContainer: Color,
    val problem: Color,
    val onProblem: Color,
    val problemContainer: Color,
    val onProblemContainer: Color,
    val neutral: Color,
    val onNeutral: Color,
    val neutralContainer: Color,
    val onNeutralContainer: Color,
    val info: Color,
    val infoContainer: Color,
    val onInfoContainer: Color,
)

private val LightStatusColors = StatusColors(
    ready = TanescoPalette.LightReady,
    onReady = TanescoPalette.LightOnReady,
    readyContainer = TanescoPalette.LightReadyContainer,
    onReadyContainer = TanescoPalette.LightOnReadyContainer,
    attention = TanescoPalette.LightAttention,
    onAttention = TanescoPalette.LightOnAttention,
    attentionContainer = TanescoPalette.LightAttentionContainer,
    onAttentionContainer = TanescoPalette.LightOnAttentionContainer,
    problem = TanescoPalette.LightProblem,
    onProblem = TanescoPalette.LightOnProblem,
    problemContainer = TanescoPalette.LightProblemContainer,
    onProblemContainer = TanescoPalette.LightOnProblemContainer,
    neutral = TanescoPalette.LightNeutral,
    onNeutral = TanescoPalette.LightOnNeutral,
    neutralContainer = TanescoPalette.LightNeutralContainer,
    onNeutralContainer = TanescoPalette.LightOnNeutralContainer,
    info = TanescoPalette.LightInfo,
    infoContainer = TanescoPalette.LightInfoContainer,
    onInfoContainer = TanescoPalette.LightOnInfoContainer,
)

private val DarkStatusColors = StatusColors(
    ready = TanescoPalette.DarkReady,
    onReady = TanescoPalette.DarkOnReady,
    readyContainer = TanescoPalette.DarkReadyContainer,
    onReadyContainer = TanescoPalette.DarkOnReadyContainer,
    attention = TanescoPalette.DarkAttention,
    onAttention = TanescoPalette.DarkOnAttention,
    attentionContainer = TanescoPalette.DarkAttentionContainer,
    onAttentionContainer = TanescoPalette.DarkOnAttentionContainer,
    problem = TanescoPalette.DarkProblem,
    onProblem = TanescoPalette.DarkOnProblem,
    problemContainer = TanescoPalette.DarkProblemContainer,
    onProblemContainer = TanescoPalette.DarkOnProblemContainer,
    neutral = TanescoPalette.DarkNeutral,
    onNeutral = TanescoPalette.DarkOnNeutral,
    neutralContainer = TanescoPalette.DarkNeutralContainer,
    onNeutralContainer = TanescoPalette.DarkOnNeutralContainer,
    info = TanescoPalette.DarkInfo,
    infoContainer = TanescoPalette.DarkInfoContainer,
    onInfoContainer = TanescoPalette.DarkOnInfoContainer,
)

private val LocalStatusColors = staticCompositionLocalOf { LightStatusColors }

private val LightColors = lightColorScheme(
    primary = TanescoPalette.LightPrimary,
    onPrimary = TanescoPalette.LightOnPrimary,
    primaryContainer = TanescoPalette.LightPrimaryContainer,
    onPrimaryContainer = TanescoPalette.LightOnPrimaryContainer,
    secondary = TanescoPalette.LightNeutral,
    onSecondary = TanescoPalette.LightOnNeutral,
    secondaryContainer = TanescoPalette.LightNeutralContainer,
    onSecondaryContainer = TanescoPalette.LightOnNeutralContainer,
    tertiary = TanescoPalette.LightInfo,
    onTertiary = Color.White,
    tertiaryContainer = TanescoPalette.LightInfoContainer,
    onTertiaryContainer = TanescoPalette.LightOnInfoContainer,
    error = TanescoPalette.LightProblem,
    onError = TanescoPalette.LightOnProblem,
    errorContainer = TanescoPalette.LightProblemContainer,
    onErrorContainer = TanescoPalette.LightOnProblemContainer,
    background = TanescoPalette.LightBackground,
    onBackground = TanescoPalette.LightOnSurface,
    surface = TanescoPalette.LightSurface,
    onSurface = TanescoPalette.LightOnSurface,
    surfaceVariant = TanescoPalette.LightSurfaceMuted,
    onSurfaceVariant = TanescoPalette.LightOnSurfaceMuted,
    surfaceContainerLowest = TanescoPalette.LightSurface,
    surfaceContainerLow = TanescoPalette.LightSurface,
    surfaceContainer = TanescoPalette.LightSurfaceMuted,
    surfaceContainerHigh = TanescoPalette.LightSurfaceMuted,
    surfaceContainerHighest = TanescoPalette.LightSurfaceSunken,
    surfaceTint = TanescoPalette.LightPrimary,
    outline = TanescoPalette.LightOutline,
    outlineVariant = TanescoPalette.LightSurfaceMuted,
    scrim = Color.Black,
    inverseSurface = TanescoPalette.LightOnSurface,
    inverseOnSurface = TanescoPalette.LightBackground,
    inversePrimary = TanescoPalette.DarkPrimary,
)

private val DarkColors = darkColorScheme(
    primary = TanescoPalette.DarkPrimary,
    onPrimary = TanescoPalette.DarkOnPrimary,
    primaryContainer = TanescoPalette.DarkPrimaryContainer,
    onPrimaryContainer = TanescoPalette.DarkOnPrimaryContainer,
    secondary = TanescoPalette.DarkNeutral,
    onSecondary = TanescoPalette.DarkOnNeutral,
    secondaryContainer = TanescoPalette.DarkNeutralContainer,
    onSecondaryContainer = TanescoPalette.DarkOnNeutralContainer,
    tertiary = TanescoPalette.DarkInfo,
    onTertiary = Color(0xFF10222E),
    tertiaryContainer = TanescoPalette.DarkInfoContainer,
    onTertiaryContainer = TanescoPalette.DarkOnInfoContainer,
    error = TanescoPalette.DarkProblem,
    onError = TanescoPalette.DarkOnProblem,
    errorContainer = TanescoPalette.DarkProblemContainer,
    onErrorContainer = TanescoPalette.DarkOnProblemContainer,
    background = TanescoPalette.DarkBackground,
    onBackground = TanescoPalette.DarkOnSurface,
    surface = TanescoPalette.DarkSurface,
    onSurface = TanescoPalette.DarkOnSurface,
    surfaceVariant = TanescoPalette.DarkSurfaceMuted,
    onSurfaceVariant = TanescoPalette.DarkOnSurfaceMuted,
    surfaceContainerLowest = TanescoPalette.DarkSurfaceSunken,
    surfaceContainerLow = TanescoPalette.DarkSurface,
    surfaceContainer = TanescoPalette.DarkSurfaceMuted,
    surfaceContainerHigh = TanescoPalette.DarkSurfaceMuted,
    surfaceContainerHighest = TanescoPalette.DarkSurfaceMuted,
    surfaceTint = TanescoPalette.DarkPrimary,
    outline = TanescoPalette.DarkOutline,
    outlineVariant = TanescoPalette.DarkSurfaceMuted,
    scrim = Color.Black,
    inverseSurface = TanescoPalette.DarkOnSurface,
    inverseOnSurface = TanescoPalette.DarkBackground,
    inversePrimary = TanescoPalette.LightPrimary,
)

/**
 * The application theme.
 *
 * Dynamic colour (Material You) is intentionally **not** used. A utility
 * application needs a predictable, consistent appearance: the status palette
 * below carries meaning, and letting the wallpaper repaint it would undermine
 * the readability contract that the whole design system is built on.
 */
@Composable
fun TanescoTheme(
    appearance: AppearanceMode = AppearanceMode.System,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appearance) {
        AppearanceMode.System -> isSystemInDarkTheme()
        AppearanceMode.Light -> false
        AppearanceMode.Dark -> true
    }

    CompositionLocalProvider(
        LocalStatusColors provides if (darkTheme) DarkStatusColors else LightStatusColors,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = TanescoTypography,
            shapes = TanescoShapes,
            content = content,
        )
    }
}

/** Accessor for the semantic status palette. */
object TanescoTheme {
    val status: StatusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalStatusColors.current
}
