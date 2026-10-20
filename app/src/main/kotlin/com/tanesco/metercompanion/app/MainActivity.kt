package com.tanesco.metercompanion.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tanesco.metercompanion.app.designsystem.theme.TanescoTheme
import com.tanesco.metercompanion.app.ui.navigation.TanescoApp
import com.tanesco.metercompanion.core.model.AppSettings

/**
 * The single activity.
 *
 * A single-activity structure is deliberate: every screen is a composable
 * destination, so there is exactly one place where the theme, the insets and the
 * container are wired, and no screen can accidentally opt out of them.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Called before setContent so the system bars are transparent and the
        // content draws behind them from the first frame, avoiding a flash of a
        // differently coloured bar.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as MeterCompanionApplication).container

        setContent {
            // The theme follows the user's appearance setting, so changing it in
            // Settings applies immediately without recreating the activity.
            val settings by container.settingsRepository
                .observe()
                .collectAsStateWithLifecycle(initialValue = AppSettings.Default)

            TanescoTheme(appearance = settings.appearance) {
                TanescoApp(container = container)
            }
        }
    }
}
