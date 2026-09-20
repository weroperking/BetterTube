package com.bettertube.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bettertube.app.data.preferences.OnboardingPreferences
import com.bettertube.app.data.share.ShareIntentHandler
import com.bettertube.app.domain.model.VaultState
import com.bettertube.app.domain.repository.VaultRepository
import com.bettertube.app.ui.navigation.BetterTubeNavGraph
import com.bettertube.app.ui.navigation.BottomNavItem
import com.bettertube.app.ui.navigation.Screen
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BgSecondary
import com.bettertube.app.ui.theme.BrandPrimary
import com.bettertube.app.ui.theme.SurfaceDark
import com.bettertube.app.ui.theme.TextSecondary
import com.example.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var shareIntentHandler: ShareIntentHandler

    @Inject
    lateinit var vaultRepository: VaultRepository

    @Inject
    lateinit var onboardingPreferences: OnboardingPreferences

    private val pendingSharedUrl = mutableStateOf<String?>(null)
    private val pendingDestination = mutableStateOf<String?>(null)
    private var backgroundedAtMillis: Long = 0L
    private var isAppReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isAppReady }

        super.onCreate(savedInstanceState)
        val initialUrl = shareIntentHandler.handleIntent(intent)
        pendingSharedUrl.value = initialUrl

        if (intent?.getStringExtra("navigate_to") == "downloads") {
            pendingDestination.value = Screen.Downloads.route
        }

        val startDest = if (onboardingPreferences.hasCompleted()) {
            Screen.Home.route
        } else {
            Screen.Onboarding.route
        }

        enableEdgeToEdge()
        setContent {
            BetterTubeTheme {
                MainAppScreen(
                    startDestination = startDest,
                    pendingSharedUrl = pendingSharedUrl.value,
                    onSharedUrlConsumed = {
                        pendingSharedUrl.value = null
                    },
                    pendingDestination = pendingDestination.value,
                    onDestinationConsumed = {
                        pendingDestination.value = null
                    },
                    onReady = {
                        isAppReady = true
                    }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        backgroundedAtMillis = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        if (backgroundedAtMillis > 0L) {
            val elapsed = System.currentTimeMillis() - backgroundedAtMillis
            if (elapsed > 60_000L && vaultRepository.getVaultState().value == VaultState.UNLOCKED) {
                vaultRepository.lock()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val url = shareIntentHandler.handleIntent(intent)
        pendingSharedUrl.value = url

        if (intent.getStringExtra("navigate_to") == "downloads") {
            pendingDestination.value = Screen.Downloads.route
        }
    }
}

@Composable
fun MainAppScreen(
    startDestination: String = Screen.Home.route,
    pendingSharedUrl: String? = null,
    onSharedUrlConsumed: () -> Unit = {},
    pendingDestination: String? = null,
    onDestinationConsumed: () -> Unit = {},
    onReady: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showNotificationRationale by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        onReady()
    }

    // Request POST_NOTIFICATIONS on API 33+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                showNotificationRationale = true
            }
        }

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (showNotificationRationale) {
        AlertDialog(
            onDismissRequest = { showNotificationRationale = false },
            title = { Text(text = stringResource(R.string.notification_permission_title)) },
            text = { Text(text = stringResource(R.string.notification_permission_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationRationale = false
                        val intent = Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.testTag("notification_dialog_settings_button")
                ) {
                    Text(text = stringResource(R.string.open_settings))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNotificationRationale = false },
                    modifier = Modifier.testTag("notification_dialog_dismiss_button")
                ) {
                    Text(text = stringResource(R.string.dismiss))
                }
            },
            modifier = Modifier.testTag("notification_permission_dialog")
        )
    }

    // Handle deep navigation from notifications
    LaunchedEffect(pendingDestination) {
        pendingDestination?.let { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            onDestinationConsumed()
        }
    }

    val navItems = listOf(
        BottomNavItem(route = Screen.Home.route, icon = Icons.Default.Home, label = "Home"),
        BottomNavItem(route = Screen.Downloads.route, icon = Icons.Default.Download, label = "Downloads"),
        BottomNavItem(route = Screen.Music.route, icon = Icons.Default.MusicNote, label = "Music"),
        BottomNavItem(route = Screen.Files.route, icon = Icons.Default.Folder, label = "Vault")
    )

    val isDark = isSystemInDarkTheme()
    val barBackground = if (isDark) SurfaceDark else BgSecondary
    val showBottomBar = currentRoute != Screen.Onboarding.route && currentRoute?.startsWith(Screen.Browser.route) != true

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = barBackground,
                    modifier = Modifier.testTag("bottom_navigation_bar")
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(text = item.label)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandPrimary,
                                selectedTextColor = BrandPrimary,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextSecondary,
                                indicatorColor = Color.Transparent
                            ),
                            modifier = Modifier.testTag("nav_item_${item.label.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        BetterTubeNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            startDestination = startDestination,
            pendingSharedUrl = pendingSharedUrl,
            onSharedUrlConsumed = onSharedUrlConsumed,
            onPlatformClicked = { platform ->
                if (platform.name.equals("More", ignoreCase = true) || platform.urlScheme.isBlank()) {
                    navController.navigate("browser/https%3A%2F%2Fwww.google.com")
                } else {
                    navController.navigate("browser/https%3A%2F%2F" + platform.urlScheme)
                }
            },
            onSearchBarClicked = {
                navController.navigate("browser/https%3A%2F%2Fwww.google.com")
            }
        )
    }
}
