package com.tanesco.metercompanion.app.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.tanesco.metercompanion.app.designsystem.component.BrandMark
import com.tanesco.metercompanion.app.designsystem.component.PremiumBackground
import com.tanesco.metercompanion.app.designsystem.component.glassSurface
import com.tanesco.metercompanion.app.designsystem.theme.Radii
import com.tanesco.metercompanion.app.designsystem.theme.Spacing
import com.tanesco.metercompanion.app.designsystem.theme.TanescoTheme

/**
 * The shared screen frame — a floating glass app bar over the aurora.
 *
 * Every screen uses this, so the chrome is identical throughout: the premium
 * PNG brand mark on top-level screens, a back affordance on detail screens,
 * and a two-line lockup (TANESCO eyebrow + screen title). The bar floats with
 * inset corners instead of spanning edge-to-edge like a web page header —
 * content scrolls *under* the glass, which is what makes the app read as a
 * native instrument rather than a rendered page.
 *
 * The title remains a semantic heading for screen-reader navigation, and
 * content still receives the scaffold padding (no double insets).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        PremiumBackground()
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Box(
                    Modifier
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                        .glassSurface(RoundedCornerShape(Radii.lg), elevation = 12.dp),
                ) {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(Spacing.md),
                            ) {
                                if (onNavigateBack == null) {
                                    BrandMark(size = 44.dp)
                                }
                                Column {
                                    Text(
                                        text = if (onNavigateBack == null) "TANESCO" else "TANESCO · METER COMPANION",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TanescoTheme.glass.brandHighlight,
                                    )
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier.semantics { heading() },
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            if (onNavigateBack != null) {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Go back",
                                    )
                                }
                            }
                        },
                        actions = { actions() },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            },
            bottomBar = bottomBar,
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.systemBars,
        ) { padding ->
            content(padding)
        }
    }
}

/**
 * The standard page body: fills the screen, applies the scaffold padding and
 * the application's page gutter.
 */
@Composable
fun ScreenColumn(
    padding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = Spacing.gutter),
        content = content,
    )
}
