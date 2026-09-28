package com.bettertube.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bettertube.app.ui.screens.browser.BrowserScreen
import com.bettertube.app.ui.screens.downloads.DownloadsScreen
import com.bettertube.app.ui.screens.files.FilesScreen
import com.bettertube.app.ui.screens.home.HomeScreen
import com.bettertube.app.ui.screens.home.model.Platform
import com.bettertube.app.ui.screens.music.MusicScreen
import com.bettertube.app.ui.screens.onboarding.OnboardingScreen
import com.bettertube.app.ui.screens.settings.SettingsScreen

@Composable
fun BetterTubeNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route,
    pendingSharedUrl: String? = null,
    isOnboardingComplete: Boolean = false,
    onSharedUrlConsumed: () -> Unit = {},
    onPlatformClicked: (Platform) -> Unit = {},
    onSearchBarClicked: () -> Unit = {}
) {
    var activeSharedUrl by remember { mutableStateOf<String?>(null) }
    var pendingUrlAfterOnboarding by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pendingSharedUrl, isOnboardingComplete) {
        if (!pendingSharedUrl.isNullOrBlank()) {
            if (isOnboardingComplete) {
                activeSharedUrl = pendingSharedUrl
                if (navController.currentDestination?.route != Screen.Home.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
                onSharedUrlConsumed()
            } else {
                pendingUrlAfterOnboarding = pendingSharedUrl
            }
        }
    }

    LaunchedEffect(isOnboardingComplete) {
        if (isOnboardingComplete && pendingUrlAfterOnboarding != null) {
            activeSharedUrl = pendingUrlAfterOnboarding
            if (navController.currentDestination?.route != Screen.Home.route) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
            onSharedUrlConsumed()
            pendingUrlAfterOnboarding = null
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(navController = navController)
        }
        composable(Screen.Home.route) {
            HomeScreen(
                navController = navController,
                sharedUrl = activeSharedUrl,
                onSharedUrlConsumed = { activeSharedUrl = null },
                onPlatformClicked = onPlatformClicked,
                onSearchBarClicked = onSearchBarClicked
            )
        }
        composable(Screen.Downloads.route) {
            DownloadsScreen(navController = navController)
        }
        composable(Screen.Music.route) {
            MusicScreen()
        }
        composable(Screen.Files.route) {
            FilesScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(
            route = "${Screen.Browser.route}/{initialUrl}",
            arguments = listOf(
                navArgument("initialUrl") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val initialUrlArg = backStackEntry.arguments?.getString("initialUrl").orEmpty()
            val initialUrl = Uri.decode(initialUrlArg)
            BrowserScreen(
                initialUrl = initialUrl,
                navController = navController,
                onDownloadDetected = { detectedUrl ->
                    activeSharedUrl = detectedUrl
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
