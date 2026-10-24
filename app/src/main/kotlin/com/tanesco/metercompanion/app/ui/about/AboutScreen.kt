package com.tanesco.metercompanion.app.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tanesco.metercompanion.app.R
import com.tanesco.metercompanion.app.designsystem.component.HairlineDivider
import com.tanesco.metercompanion.app.designsystem.component.LabeledValueRow
import com.tanesco.metercompanion.app.designsystem.component.NoticePanel
import com.tanesco.metercompanion.app.designsystem.component.SectionHeader
import com.tanesco.metercompanion.app.designsystem.component.SurfacePanel
import com.tanesco.metercompanion.app.designsystem.theme.Spacing
import com.tanesco.metercompanion.app.designsystem.theme.TanescoTheme
import com.tanesco.metercompanion.core.model.AppMetadata
import com.tanesco.metercompanion.core.model.RuntimeEnvironment
import com.tanesco.metercompanion.core.time.formatIso8601Utc

/**
 * About and technical information.
 *
 * Deliberately states the project's scope in full, including what the connection
 * layer does *not* do. For an application that records prepaid tokens, an accurate
 * statement of capability is part of the product, not a disclaimer bolted on.
 */
@Composable
fun AboutScreen(
    metadata: AppMetadata?,
    environment: RuntimeEnvironment?,
    connectionAdapterName: String?,
    tokenFormatProfileId: String?,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = Spacing.gutter),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        SurfacePanel {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Image(
                    painter = painterResource(
                        if (TanescoTheme.glass.isDark) R.drawable.brand_lockup else R.drawable.brand_lockup_light,
                    ),
                    contentDescription = "TANESCO Meter Companion logo",
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.CenterHorizontally),
                )
                SectionHeader(title = "Application")
                Text(
                    text = "TANESCO Meter Companion",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "A local-first companion for managing prepaid meter records and " +
                        "entering tokens. Every feature works offline on this device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HairlineDivider()
                LabeledValueRow(label = "Version", value = metadata?.displayVersion ?: "—")
                LabeledValueRow(label = "Package", value = metadata?.packageName ?: "—", monospaceValue = true)
            }
        }

        NoticePanel(
            title = "Meter connection is not implemented",
            body = "This build performs no communication with a meter or CIU. Reading " +
                "measurements, confirming a token and submitting credit all require a " +
                "connection adapter that does not exist yet. Tokens entered here are " +
                "recorded locally and checked for formatting only.",
        )

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SectionHeader(title = "Technical information")
            SurfacePanel {
                Column {
                    LabeledValueRow(
                        label = "Connection adapter",
                        value = connectionAdapterName ?: "none registered",
                        monospaceValue = true,
                    )
                    LabeledValueRow(
                        label = "Token format profile",
                        value = tokenFormatProfileId ?: "—",
                        monospaceValue = true,
                    )
                    environment?.let { env ->
                        LabeledValueRow(label = "Device", value = env.summary)
                        LabeledValueRow(
                            label = "Architecture",
                            value = env.primaryAbi ?: "unknown",
                            monospaceValue = true,
                        )
                        LabeledValueRow(
                            label = "64-bit",
                            value = if (env.is64Bit) "yes" else "no",
                        )
                    }
                    metadata?.firstInstalledAtMillis?.let { installed ->
                        LabeledValueRow(
                            label = "Installed",
                            value = formatIso8601Utc(installed),
                            monospaceValue = true,
                        )
                    }
                    metadata?.installerPackage?.let { installer ->
                        LabeledValueRow(
                            label = "Installed by",
                            value = installer,
                            monospaceValue = true,
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            SectionHeader(title = "Privacy")
            SurfacePanel {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        text = "Everything this application knows is stored on this device in " +
                            "its own database. There is no account, no analytics and no " +
                            "network request anywhere in the application.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Meter authentication material is never requested, stored or " +
                            "derived. The application has no mechanism to extract it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
