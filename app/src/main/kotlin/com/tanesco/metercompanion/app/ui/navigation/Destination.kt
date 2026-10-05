package com.tanesco.metercompanion.app.ui.navigation

/**
 * Every destination in the application.
 *
 * Routes are declared as constants rather than built from arguments at the call
 * site so that a typo is a compile error in one place instead of a runtime
 * crash. Routes that take an argument expose a builder function.
 */
sealed class Destination(val route: String) {

    // --- Primary tabs ------------------------------------------------------
    data object Home : Destination("home")
    data object Meter : Destination("meter")
    data object Tokens : Destination("tokens")
    data object Activity : Destination("activity")
    data object Settings : Destination("settings")

    // --- Secondary screens -------------------------------------------------
    data object Diagnostics : Destination("diagnostics")
    data object About : Destination("about")

    /** Meter Setup, optionally editing an existing profile. */
    data object MeterSetup : Destination("meter/setup?profileId={profileId}") {
        const val ARG_PROFILE_ID = "profileId"
        const val ROUTE_PATTERN = "meter/setup?profileId={profileId}"

        /** Route for creating a new profile. */
        fun createRoute(): String = "meter/setup"

        /** Route for editing [profileId]. */
        fun editRoute(profileId: String): String = "meter/setup?profileId=$profileId"
    }

    companion object {
        /** Routes shown in the bottom navigation bar, in display order. */
        val primaryTabs: List<Destination> = listOf(Home, Meter, Tokens, Activity, Settings)
    }
}

/** Title shown in the top bar for each destination. */
fun Destination.title(): String = when (this) {
    Destination.Home -> "Overview"
    Destination.Meter -> "Meter"
    Destination.Tokens -> "Token Center"
    Destination.Activity -> "Activity"
    Destination.Settings -> "Settings"
    Destination.Diagnostics -> "Diagnostics"
    Destination.About -> "About"
    Destination.MeterSetup -> "Meter setup"
}
