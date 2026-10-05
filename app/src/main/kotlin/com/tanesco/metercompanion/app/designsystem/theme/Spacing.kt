package com.tanesco.metercompanion.app.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing scale.
 *
 * A scale rather than ad-hoc values is what makes layouts look deliberate. Every
 * gap in the application is one of these, which is why vertical rhythm is
 * consistent between screens without anyone tuning each screen by hand.
 *
 * The scale is 4dp-based with a few 2dp and 6dp steps for optical corrections
 * inside dense rows.
 */
object Spacing {

    /** Hairline separations and icon-to-label gaps. */
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
    val huge: Dp = 40.dp
    val giant: Dp = 56.dp

    /**
     * Horizontal screen gutter.
     *
     * 16dp keeps content clear of the edges on a 360dp-wide phone while still
     * allowing a two-column grid on larger screens. Larger layouts increase this
     * rather than stretching content, which keeps line lengths readable.
     */
    val gutter: Dp = 16.dp

    /** Gutter used once the window is wide enough to be a tablet layout. */
    val gutterWide: Dp = 24.dp

    /** Maximum width for a reading column on a large screen. */
    val contentMaxWidth: Dp = 640.dp
}

/** Corner radii. Restrained: this is tooling, not a toy. */
object Radii {
    val xs: Dp = 6.dp
    val sm: Dp = 10.dp
    val md: Dp = 14.dp
    val lg: Dp = 20.dp
    val xl: Dp = 28.dp
    val pill: Dp = 999.dp
}

/** Material shapes wired to [Radii]. */
val TanescoShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(Radii.xs),
    small = RoundedCornerShape(Radii.sm),
    medium = RoundedCornerShape(Radii.md),
    large = RoundedCornerShape(Radii.lg),
    extraLarge = RoundedCornerShape(Radii.xl),
)

/** Sizes shared by more than one component. */
object ComponentSize {
    /** Minimum interactive height. Meets the Material accessibility guidance. */
    val minTouchTarget: Dp = 48.dp

    /** Height of a standard button. */
    val buttonHeight: Dp = 52.dp

    /** Height of a compact inline button used inside list rows. */
    val buttonHeightCompact: Dp = 40.dp

    /** Thickness of the leading accent rule on a status panel. */
    val accentRuleWidth: Dp = 3.dp

    /** Width of a hairline divider. */
    val hairline: Dp = 1.dp
}
