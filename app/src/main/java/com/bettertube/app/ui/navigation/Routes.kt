package com.bettertube.app.ui.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Downloads : Screen("downloads")
    data object Music : Screen("music")
    data object Files : Screen("files")
    data object Browser : Screen("browser")
    data object Settings : Screen("settings")
}
