package com.tanesco.metercompanion.app.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tanesco.metercompanion.app.designsystem.theme.Radii
import com.tanesco.metercompanion.app.designsystem.theme.Spacing
import com.tanesco.metercompanion.app.designsystem.theme.TanescoTheme
import com.tanesco.metercompanion.core.diagnostics.DiagnosticStatus

/**
 * The visual vocabulary for subsystem health.
 *
 * Every status in the application resolves to exactly one of these appearances,
 * so "Ready" looks the same on Home, on the Meter screen and in Diagnostics.
 * Centralising the mapping is what stops a research state from being rendered
 * as a warning in one place and as a success in another.
 */
data class StatusStyle(
    val label: String,
    val accent: Color,
    val container: Color,
    val onContainer: Color,
    /** True when a filled dot is used; hollow means "not a problem, not active". */
    val emphasised: Boolean,
)

@Composable
fun DiagnosticStatus.style(): StatusStyle {
    val status = TanescoTheme.status
    return when (this) {
        DiagnosticStatus.Ready -> StatusStyle(
            label = label,
            accent = status.ready,
            container = status.readyContainer,
            onContainer = status.onReadyContainer,
            emphasised = true,
        )

        DiagnosticStatus.Available -> StatusStyle(
            label = label,
            accent = status.neutral,
            container = status.neutralContainer,
            onContainer = status.onNeutralContainer,
            emphasised = false,
        )

        DiagnosticStatus.Research -> StatusStyle(
            label = label,
            accent = status.attention,
            container = status.attentionContainer,
            onContainer = status.onAttentionContainer,
            emphasised = false,
        )

        DiagnosticStatus.Unavailable -> StatusStyle(
            label = label,
            accent = status.neutral,
            container = status.neutralContainer,
            onContainer = status.onNeutralContainer,
            emphasised = false,
        )

        DiagnosticStatus.Error -> StatusStyle(
            label = label,
            accent = status.problem,
            container = status.problemContainer,
            onContainer = status.onProblemContainer,
            emphasised = true,
        )
    }
}

/**
 * A compact status chip: dot plus label.
 *
 * The dot is hollow for states that are neither healthy nor faulty. That
 * distinction is the point - a hollow dot reads as "deliberately not
 * implemented", which is exactly what a research state is, and it never reads
 * as a warning or as a success.
 *
 * The chip is a single accessibility node announcing "label, status", so a
 * screen reader user is not read a bare colour and a bare word separately.
 */
@Composable
fun StatusPill(
    label: String,
    status: DiagnosticStatus,
    modifier: Modifier = Modifier,
) {
    val style = status.style()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(Radii.pill))
            .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                    listOf(style.container, style.container.copy(alpha = 0.62f)),
                ),
            )
            .border(1.dp, style.accent.copy(alpha = 0.45f), RoundedCornerShape(Radii.pill))
            .padding(horizontal = Spacing.md, vertical = Spacing.xs + Spacing.xxs)
            .semantics { contentDescription = "$label, status ${style.label}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            Modifier
                .size(Spacing.sm)
                .clip(CircleShape)
                .background(if (style.emphasised) style.accent else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = style.accent,
                    shape = CircleShape,
                ),
        )
        Text(
            text = style.label,
            style = MaterialTheme.typography.labelMedium,
            color = style.onContainer,
            maxLines = 1,
        )
    }
}

/**
 * A status block: a label, a status chip, and an explanatory sentence.
 *
 * This is the shape used wherever the application must explain *why* something
 * is in its current state - most importantly on Home and Meter, where "not
 * connected" needs a reason rather than a bare word.
 */
@Composable
fun StatusExplainer(
    title: String,
    status: DiagnosticStatus,
    explanation: String,
    modifier: Modifier = Modifier,
    footnote: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusPill(label = title, status = status)
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (footnote != null) {
            Text(
                text = footnote,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A short, always-visible summary of one row's state.
 *
 * Used in lists where a full explainer would be too heavy.
 */
@Composable
fun StatusText(
    label: String,
    status: DiagnosticStatus,
    modifier: Modifier = Modifier,
) {
    val style = status.style()
    Row(
        modifier = modifier.semantics { contentDescription = "$label, ${style.label}" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            Modifier
                .size(Spacing.sm)
                .clip(CircleShape)
                .background(if (style.emphasised) style.accent else Color.Transparent)
                .border(1.5.dp, style.accent, CircleShape),
        )
        Text(
            text = style.label,
            style = MaterialTheme.typography.bodyMedium,
            color = style.onContainer,
        )
    }
}

/**
 * Centred status message used by empty and unavailable states.
 *
 * Constrained to a readable measure and centred, so a short message on a small
 * phone does not stretch into one line per word.
 */
@Composable
fun StatusMessage(
    status: DiagnosticStatus,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    val style = status.style()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 360.dp)
            .padding(vertical = Spacing.xxl)
            .semantics(mergeDescendants = true) { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        StatusPill(label = title, status = status)
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Box(Modifier.padding(top = Spacing.sm)) { action() }
        }
    }
}

/**
 * Decorative dot used inside dense rows where a full chip would be too heavy.
 *
 * Carries no semantics of its own; the row it sits in provides the label.
 */
@Composable
fun StatusDot(status: DiagnosticStatus, modifier: Modifier = Modifier) {
    val style = status.style()
    Box(
        modifier
            .size(Spacing.sm)
            .clip(CircleShape)
            .background(if (style.emphasised) style.accent else Color.Transparent)
            .border(1.5.dp, style.accent, CircleShape)
            .clearAndSetSemantics { },
    )
}
