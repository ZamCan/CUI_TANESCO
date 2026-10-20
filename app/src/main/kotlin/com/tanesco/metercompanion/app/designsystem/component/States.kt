package com.tanesco.metercompanion.app.designsystem.component

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tanesco.metercompanion.app.designsystem.theme.Radii
import com.tanesco.metercompanion.app.designsystem.theme.Spacing
import com.tanesco.metercompanion.core.diagnostics.DiagnosticStatus

/**
 * Indeterminate progress for a whole screen section.
 *
 * A linear bar rather than a centred spinner: this application's loading states
 * are almost always "reading a small local database", which resolves in
 * milliseconds. A spinner that flashes for 80 ms reads as jank, whereas a thin
 * bar at the top of the section keeps the layout still.
 */
@Composable
fun SectionLoadingBar(
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clearAndSetSemantics { },
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = Spacing.sm)
                .semantics { contentDescription = label },
        )
    }
}

/**
 * Skeleton placeholder used while a list loads.
 *
 * Shaped like the content it stands in for so the layout does not jump when the
 * data arrives. Marked decorative: a screen reader should hear the loading
 * label once, not once per placeholder.
 */
@Composable
fun SkeletonRow(
    modifier: Modifier = Modifier,
    lines: Int = 2,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md)
            .clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        repeat(lines) { index ->
            Box(
                Modifier
                    .fillMaxWidth(if (index == 0) 0.55f else 0.85f)
                    .height(if (index == 0) 14.dp else 11.dp)
                    .clip(RoundedCornerShape(Radii.xs))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .alpha(alpha),
            )
        }
    }
}

/**
 * The empty state.
 *
 * States *what is missing and what to do about it*. An empty screen with no
 * explanation is the single most common way an application feels broken, so
 * every list in this application supplies an action here.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    status: DiagnosticStatus = DiagnosticStatus.Available,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xl)
            .semantics(mergeDescendants = true) { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        StatusPill(label = title, status = status)
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Spacing.md),
        )
        if (action != null) {
            Box(Modifier.padding(top = Spacing.xs)) { action() }
        }
    }
}

/**
 * An error the user can act on.
 *
 * Always offers a way forward. A failure with no recovery path is a dead end,
 * so either [onRetry] or [action] is expected to be supplied.
 */
@Composable
fun ErrorState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "Try again",
    action: @Composable (() -> Unit)? = null,
) {
    AccentPanel(
        accentColor = MaterialTheme.colorScheme.error,
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onRetry != null) {
                Box(Modifier.padding(top = Spacing.xs)) {
                    SecondaryActionButton(text = retryLabel, onClick = onRetry)
                }
            }
            if (action != null) {
                Box(Modifier.padding(top = Spacing.xs)) { action() }
            }
        }
    }
}

/**
 * An inline notice for a non-fatal condition - a warning, a research notice.
 *
 * Used where the application wants to inform without alarming: the meter screen
 * explaining that connection support is under development, for example.
 */
@Composable
fun NoticePanel(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    status: DiagnosticStatus = DiagnosticStatus.Research,
    action: @Composable (() -> Unit)? = null,
) {
    val style = status.style()
    AccentPanel(accentColor = style.accent, modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = style.onContainer,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (action != null) {
                Box(Modifier.padding(top = Spacing.xs)) { action() }
            }
        }
    }
}

/**
 * A single-line message shown above content, e.g. after a successful save.
 *
 * Confirms only what actually happened. Copy such as "Token sent to meter" may
 * not be used here unless a connection adapter reported it.
 */
@Composable
fun InlineConfirmation(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.sm))
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .padding(horizontal = Spacing.md, vertical = Spacing.md)
            .semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(Radii.pill))
                .background(MaterialTheme.colorScheme.onTertiaryContainer),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

/** Fixed-width spacer used between inline elements in a row. */
@Composable
fun HSpace(width: androidx.compose.ui.unit.Dp) {
    Box(Modifier.width(width))
}
