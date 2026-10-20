package com.tanesco.metercompanion.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tanesco.metercompanion.app.AppContainer
import com.tanesco.metercompanion.app.ui.about.AboutScreen
import com.tanesco.metercompanion.app.ui.activity.ActivityScreen
import com.tanesco.metercompanion.app.ui.activity.ActivityViewModel
import com.tanesco.metercompanion.app.ui.common.ScreenScaffold
import com.tanesco.metercompanion.app.ui.common.containerViewModel
import com.tanesco.metercompanion.app.ui.diagnostics.DiagnosticsScreen
import com.tanesco.metercompanion.app.ui.diagnostics.DiagnosticsViewModel
import com.tanesco.metercompanion.app.ui.home.HomeScreen
import com.tanesco.metercompanion.app.ui.home.HomeViewModel
import com.tanesco.metercompanion.app.ui.meter.MeterScreen
import com.tanesco.metercompanion.app.ui.meter.MeterViewModel
import com.tanesco.metercompanion.app.ui.metersetup.MeterSetupScreen
import com.tanesco.metercompanion.app.ui.metersetup.MeterSetupViewModel
import com.tanesco.metercompanion.app.ui.settings.SettingsScreen
import com.tanesco.metercompanion.app.ui.settings.SettingsViewModel
import com.tanesco.metercompanion.app.ui.token.TokenScreen
import com.tanesco.metercompanion.app.ui.token.TokenViewModel

/**
 * The four primary destinations shown in the bottom bar.
 *
 * Four, not eight: on a 360dp phone, five tabs squeeze the labels and eight are
 * impossible. Settings, Diagnostics and About are reached from Home and from the
 * screens that need them, which keeps the frequent destinations one tap away
 * without cramping the bar.
 */
private enum class Tab(val destination: Destination, val label: String, val icon: ImageVector) {
    Home(Destination.Home, "Home", Icons.Filled.Home),
    Meter(Destination.Meter, "Meter", Icons.Filled.Settings),
    Tokens(Destination.Tokens, "Tokens", Icons.Filled.Lock),
    Activity(Destination.Activity, "Activity", Icons.Filled.List),
}

/**
 * The application shell.
 *
 * Owns the [NavHost] and the bottom bar. Screens are stateless with respect to
 * navigation: each receives its state from a container-built ViewModel and a
 * lambda, so navigation can be changed or tested without touching a screen.
 */
@Composable
fun TanescoApp(container: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination

    val showBottomBar = Tab.entries.any { tab ->
        currentRoute?.hierarchy?.any { it.route == tab.destination.route } == true
    }

    androidx.compose.material3.Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        val selected = currentRoute?.hierarchy
                            ?.any { it.route == tab.destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab.destination) },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                )
                            },
                            label = { Text(tab.label) },
                            alwaysShowLabel = false,
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Destination.Home.route) {
                val viewModel = containerViewModel<HomeViewModel>(container)
                val state by viewModel.state.collectAsStateWithLifecycle()
                ScreenScaffold(title = Destination.Home.title()) {
                    HomeScreen(
                        state = state,
                        onOpenMeter = { navController.navigate(Destination.Meter.route) },
                        onOpenTokens = { navController.navigate(Destination.Tokens.route) },
                        onOpenActivity = { navController.navigate(Destination.Activity.route) },
                        onOpenSettings = { navController.navigate(Destination.Settings.route) },
                        onOpenDiagnostics = { navController.navigate(Destination.Diagnostics.route) },
                        onOpenAbout = { navController.navigate(Destination.About.route) },
                        onAddMeter = { navController.navigate(Destination.MeterSetup.createRoute()) },
                        contentPadding = it,
                    )
                }
            }

            composable(Destination.Meter.route) {
                val viewModel = containerViewModel<MeterViewModel>(container)
                val state by viewModel.state.collectAsStateWithLifecycle()
                val attempt by viewModel.attempt.collectAsStateWithLifecycle()
                val working by viewModel.isWorking.collectAsStateWithLifecycle()
                ScreenScaffold(title = Destination.Meter.title()) {
                    MeterScreen(
                        state = state,
                        isWorking = working,
                        attempt = attempt,
                        onAddMeter = { navController.navigate(Destination.MeterSetup.createRoute()) },
                        onEditMeter = {
                            val id = state.meter?.id
                            navController.navigate(
                                if (id == null) {
                                    Destination.MeterSetup.createRoute()
                                } else {
                                    Destination.MeterSetup.editRoute(id)
                                },
                            )
                        },
                        onRequestConnection = viewModel::requestConnection,
                        onOpenDiagnostics = { navController.navigate(Destination.Diagnostics.route) },
                        onAttemptConsumed = viewModel::consumeAttemptResult,
                        contentPadding = it,
                    )
                }
            }

            composable(Destination.Tokens.route) {
                val viewModel = containerViewModel<TokenViewModel>(container)
                val state by viewModel.state.collectAsStateWithLifecycle()
                val submission by viewModel.submission.collectAsStateWithLifecycle()
                ScreenScaffold(title = Destination.Tokens.title()) {
                    TokenScreen(
                        state = state,
                        submission = submission,
                        onDigit = viewModel::pressDigit,
                        onCancel = viewModel::pressCancel,
                        onSubmit = viewModel::submit,
                        onPageNext = viewModel::pressPageNext,
                        onPagePrevious = viewModel::pressPagePrevious,
                        onTypedValueChanged = viewModel::onTypedValueChanged,
                        onMeterFilterChanged = viewModel::setMeterFilter,
                        onSubmissionConsumed = viewModel::consumeSubmissionResult,
                        contentPadding = it,
                    )
                }
            }

            composable(Destination.Activity.route) {
                val viewModel = containerViewModel<ActivityViewModel>(container)
                val state by viewModel.state.collectAsStateWithLifecycle()
                ScreenScaffold(title = Destination.Activity.title()) {
                    ActivityScreen(
                        state = state,
                        onFilterSelected = viewModel::selectFilter,
                        contentPadding = it,
                    )
                }
            }

            composable(Destination.Settings.route) {
                val viewModel = containerViewModel<SettingsViewModel>(container)
                val state by viewModel.state.collectAsStateWithLifecycle()
                val notice by viewModel.notice.collectAsStateWithLifecycle()
                ScreenScaffold(title = Destination.Settings.title()) {
                    SettingsScreen(
                        state = state,
                        notice = notice,
                        onAppearanceChanged = viewModel::setAppearance,
                        onUnitSystemChanged = viewModel::setUnitSystem,
                        onConnectionAttemptsChanged = viewModel::setConnectionAttemptsEnabled,
                        onDeveloperModeChanged = viewModel::setDeveloperMode,
                        onDefaultMeterChanged = viewModel::setDefaultMeter,
                        onArchiveMeter = viewModel::archiveMeter,
                        onAddMeter = { navController.navigate(Destination.MeterSetup.createRoute()) },
                        onOpenDiagnostics = { navController.navigate(Destination.Diagnostics.route) },
                        onOpenAbout = { navController.navigate(Destination.About.route) },
                        onNoticeConsumed = viewModel::consumeNotice,
                        contentPadding = it,
                    )
                }
            }

            composable(Destination.Diagnostics.route) {
                val viewModel = containerViewModel<DiagnosticsViewModel>(container)
                val state by viewModel.state.collectAsStateWithLifecycle()
                val refreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
                ScreenScaffold(
                    title = Destination.Diagnostics.title(),
                    onNavigateBack = { navController.popBackStack() },
                ) {
                    DiagnosticsScreen(
                        state = state,
                        isRefreshing = refreshing,
                        onRefresh = viewModel::refresh,
                        contentPadding = it,
                    )
                }
            }

            composable(Destination.About.route) {
                val metadata = remember(container) { container.appMetadataProvider.appMetadata() }
                val environment = remember(container) {
                    container.appMetadataProvider.runtimeEnvironment()
                }
                ScreenScaffold(
                    title = Destination.About.title(),
                    onNavigateBack = { navController.popBackStack() },
                ) {
                    AboutScreen(
                        metadata = metadata,
                        environment = environment,
                        connectionAdapterName = container.connectionProvider.current()?.id,
                        tokenFormatProfileId = "structural-20-digit-v1",
                        contentPadding = it,
                    )
                }
            }

            composable(
                route = Destination.MeterSetup.ROUTE_PATTERN,
                arguments = listOf(
                    navArgument(Destination.MeterSetup.ARG_PROFILE_ID) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val profileId = entry.arguments
                    ?.getString(Destination.MeterSetup.ARG_PROFILE_ID)
                val viewModel = containerViewModel<MeterSetupViewModel>(
                    container = container,
                    key = "meter-setup-${profileId ?: "new"}",
                )
                androidx.compose.runtime.LaunchedEffect(profileId) {
                    viewModel.start(profileId)
                }
                val state by viewModel.state.collectAsStateWithLifecycle()
                val savedProfileId = state.savedProfileId

                // Navigates back once the profile is stored, so the screen is
                // never left showing a form that has already been submitted.
                androidx.compose.runtime.LaunchedEffect(savedProfileId) {
                    if (savedProfileId != null) navController.popBackStack()
                }

                ScreenScaffold(
                    title = state.title,
                    onNavigateBack = { navController.popBackStack() },
                ) {
                    MeterSetupScreen(
                        state = state,
                        onAccountNumberChanged = viewModel::onAccountNumberChanged,
                        onDisplayNameChanged = viewModel::onDisplayNameChanged,
                        onRegionChanged = viewModel::onRegionChanged,
                        onNotesChanged = viewModel::onNotesChanged,
                        onCategoryChanged = viewModel::onCategoryChanged,
                        onPhaseChanged = viewModel::onPhaseChanged,
                        onSave = viewModel::save,
                        contentPadding = it,
                    )
                }
            }
        }
    }
}

/**
 * Switches tabs the way a bottom bar should: single instance on top, state
 * preserved per tab, and never a growing back stack.
 */
private fun NavHostController.navigateToTab(destination: Destination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
