package com.tanesco.metercompanion.app.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tanesco.metercompanion.app.R
import com.tanesco.metercompanion.app.designsystem.theme.TanescoTheme

/**
 * Premium glass primitives shared by every screen.
 *
 * Glass here is a system, not a filter: [PremiumBackground] lays an aurora
 * wash in the TANESCO green/yellow relationship, panels are translucent
 * gradient fills with a top light-catch and a gradient hairline, and floating
 * chrome (top bar, bottom navigation) uses the same fill at higher opacity so
 * text contrast is never left to whatever happens to scroll underneath.
 *
 * No blur library is used: the app must stay buildable offline on Termux and
 * smooth on low-end devices. Layered translucency over a *static* aurora gives
 * the same perceived depth at zero runtime blur cost.
 */

@Composable
fun glassFill(accent: Color? = null): Brush {
    val glass = TanescoTheme.glass
    val top = if (accent == null) glass.fillTop else {
        if (glass.isDark) accent.copy(alpha = 0.14f) else accent.copy(alpha = 0.10f)
    }
    return Brush.verticalGradient(listOf(top, glass.fillBottom))
}

@Composable
fun glassBorder(accent: Color? = null): Brush {
    val glass = TanescoTheme.glass
    return Brush.linearGradient(
        listOf(
            glass.borderLight,
            Color.White.copy(alpha = if (glass.isDark) 0.06f else 0.35f),
            accent?.copy(alpha = 0.38f) ?: glass.borderDeep,
        ),
    )
}

/** Aurora wash behind all content: sun-yellow top-right, energy-green left, sky low. */
@Composable
fun PremiumBackground(modifier: Modifier = Modifier) {
    val glass = TanescoTheme.glass
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(scheme.background, scheme.background, scheme.surfaceContainerLowest))),
    )
    Canvas(modifier.fillMaxSize()) {
        val glows = listOf(
            Triple(glass.auroraSun, Offset(size.width * 0.94f, size.height * 0.0f), size.width * 0.55f),
            Triple(glass.auroraEnergy, Offset(size.width * 0.0f, size.height * 0.36f), size.width * 0.5f),
            Triple(glass.auroraSky, Offset(size.width * 0.82f, size.height * 1.02f), size.width * 0.52f),
        )
        glows.forEach { (color, center, radius) ->
            drawCircle(
                brush = Brush.radialGradient(listOf(color, Color.Transparent), center, radius),
                radius = radius,
                center = center,
            )
        }
    }
}

/** Applies the shared glass surface treatment to any container. */
@Composable
fun Modifier.glassSurface(
    shape: Shape = RoundedCornerShape(24.dp),
    accent: Color? = null,
    elevation: Dp = 10.dp,
): Modifier = this
    .shadow(elevation, shape, ambientColor = Color.Black.copy(alpha = 0.22f),
        spotColor = (accent ?: Color(0xFF177A2E)).copy(alpha = 0.20f))
    .clip(shape)
    .background(glassFill(accent))
    .border(1.dp, glassBorder(accent), shape)

/**
 * A Material vector icon in a tinted glass medallion — the app's consistent
 * leading-icon treatment for navigation and shortcut rows. Vector icons only:
 * no emoji, no text glyphs, one optical size inside every medallion.
 */
@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.07f))))
            .border(1.dp, tint.copy(alpha = 0.42f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(size * 0.52f),
        )
    }
}

/**
 * The authentic TANESCO logo (PNG, unaltered artwork) on its proper light
 * ground. The official mark's green wordmark is designed for light surfaces,
 * so the tile is ivory in both themes — brand correct, and legible on glass.
 */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 46.dp) {
    Box(
        modifier = modifier
            .size(size)
            .shadow(6.dp, RoundedCornerShape(size * 0.30f), spotColor = Color.Black.copy(alpha = 0.25f))
            .clip(RoundedCornerShape(size * 0.30f))
            .background(Color(0xFFF7FAF1))
            .border(1.dp, Color(0xFF177A2E).copy(alpha = 0.28f), RoundedCornerShape(size * 0.30f))
            .padding(size * 0.08f),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.brand_mark),
            contentDescription = "TANESCO logo",
            modifier = Modifier.fillMaxSize(),
        )
    }
}
