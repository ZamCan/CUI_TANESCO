package com.tanesco.metercompanion.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.tanesco.metercompanion.app.designsystem.theme.Spacing

/**
 * The shared screen frame.
 *
 * Every screen uses this, so the top bar, insets and title placement are
 * identical throughout. The title is marked as a semantic heading so a screen
 * reader user can jump between sections, which matters on the longer screens
 * such as Diagnostics.
 *
 * Content receives the scaffold's padding instead of applying its own; that is
 * what prevents the double-inset bug where content ends up pushed up twice.
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
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
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
    ) { padding ->
        content(padding)
    }
}

/**
 * The standard page body: fills the screen, applies the scaffold padding and the
 * application's page gutter.
 *
 * Every screen's root content uses this, which is why vertical rhythm and edge
 * clearances are consistent without per-screen tuning.
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
