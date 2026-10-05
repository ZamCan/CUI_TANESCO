package com.tanesco.metercompanion.app.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tanesco.metercompanion.app.AppContainer
import com.tanesco.metercompanion.app.ui.activity.ActivityViewModel
import com.tanesco.metercompanion.app.ui.diagnostics.DiagnosticsViewModel
import com.tanesco.metercompanion.app.ui.home.HomeViewModel
import com.tanesco.metercompanion.app.ui.meter.MeterViewModel
import com.tanesco.metercompanion.app.ui.metersetup.MeterSetupViewModel
import com.tanesco.metercompanion.app.ui.settings.SettingsViewModel
import com.tanesco.metercompanion.app.ui.token.TokenViewModel

/**
 * Builds the [androidx.lifecycle.ViewModelProvider.Factory] for a container.
 *
 * Every ViewModel's dependencies are listed here exactly once, which makes the
 * application's entire object graph reviewable by reading a single function.
 * A DI framework would express the same thing but would add an annotation
 * processor to a build that runs on a phone; see
 * docs/decisions/0004-dependency-injection.md.
 *
 * The factory is remembered per container so that ViewModels survive recomposition
 * and are not recreated on every navigation.
 */
fun appViewModelFactory(container: AppContainer) = viewModelFactory {

    initializer {
        HomeViewModel(
            meterProfiles = container.meterProfileService,
            connectionCoordinator = container.connectionCoordinator,
            eventRepository = container.eventRepository,
            tokenRepository = container.tokenRepository,
        )
    }

    initializer {
        MeterViewModel(
            meterProfiles = container.meterProfileService,
            connectionCoordinator = container.connectionCoordinator,
            sessionRepository = container.meterSessionRepository,
            settingsRepository = container.settingsRepository,
        )
    }

    initializer {
        MeterSetupViewModel(
            meterProfiles = container.meterProfileService,
            meterRepository = container.meterRepository,
        )
    }

    initializer {
        TokenViewModel(
            tokens = container.tokenService,
            meterProfiles = container.meterProfileService,
        )
    }

    initializer {
        ActivityViewModel(
            eventRepository = container.eventRepository,
        )
    }

    initializer {
        DiagnosticsViewModel(
            diagnostics = container.diagnosticsService,
        )
    }

    initializer {
        SettingsViewModel(
            settingsRepository = container.settingsRepository,
            meterProfiles = container.meterProfileService,
            eventRecorder = container.eventRecorder,
        )
    }
}

/**
 * Obtains a ViewModel built from the application container.
 *
 * `key` distinguishes instances of the same ViewModel type where a screen needs
 * more than one (for example, one Meter Setup form per profile being edited).
 */
@Composable
inline fun <reified VM : androidx.lifecycle.ViewModel> containerViewModel(
    container: AppContainer,
    key: String? = null,
): VM {
    val factory = remember(container) { appViewModelFactory(container) }
    return viewModel(modelClass = VM::class.java, key = key, factory = factory)
}
