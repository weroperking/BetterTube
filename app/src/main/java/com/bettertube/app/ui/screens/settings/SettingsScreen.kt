package com.bettertube.app.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bettertube.app.domain.model.ProxyConfig
import com.bettertube.app.domain.model.ScheduleConfig
import com.bettertube.app.ui.screens.settings.components.CrashLogsDialog
import com.bettertube.app.ui.screens.settings.components.HeadersDialog
import com.bettertube.app.ui.screens.settings.components.NumericInputDialog
import com.bettertube.app.ui.screens.settings.components.ProxyConfigDialog
import com.bettertube.app.ui.screens.settings.components.ScheduleDaysDialog
import com.bettertube.app.ui.screens.settings.components.ScheduleTimeDialog
import com.bettertube.app.ui.screens.settings.components.SettingsRow
import com.bettertube.app.ui.screens.settings.components.SettingsSection
import com.bettertube.app.ui.screens.settings.components.SpeedLimitDialog
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BrandPrimary
import com.bettertube.app.ui.theme.TextSecondary
import com.bettertube.app.utils.FormatUtils
import com.bettertube.app.BuildConfig
import com.bettertube.app.R
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val daemonRunning by viewModel.daemonRunning.collectAsStateWithLifecycle()
    val proxyConfig by viewModel.proxyConfig.collectAsStateWithLifecycle()
    val scheduleConfig by viewModel.scheduleConfig.collectAsStateWithLifecycle()
    val globalSpeedLimit by viewModel.globalSpeedLimit.collectAsStateWithLifecycle()
    val aria2Version by viewModel.aria2Version.collectAsStateWithLifecycle()
    val aria2Features by viewModel.aria2Features.collectAsStateWithLifecycle()
    val maxPeers by viewModel.maxPeers.collectAsStateWithLifecycle()
    val seedTime by viewModel.seedTime.collectAsStateWithLifecycle()
    val customHeaders by viewModel.customHeaders.collectAsStateWithLifecycle()
    val reduceMotion by viewModel.reduceMotion.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showSpeedDialog by remember { mutableStateOf(false) }
    var showProxyDialog by remember { mutableStateOf(false) }
    var showHeadersDialog by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }
    var showDaysDialog by remember { mutableStateOf(false) }
    var showPeersDialog by remember { mutableStateOf(false) }
    var showSeedTimeDialog by remember { mutableStateOf(false) }
    var showCrashLogsDialog by remember { mutableStateOf(false) }

    val cookiePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    viewModel.importCookies(content)
                }
            } catch (e: Exception) {
                viewModel.showSnackbar("Failed to read cookie file: ${e.message}")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController?.navigateUp() },
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .testTag("settings_content_scroll")
        ) {
            // Power Downloads Section
            SettingsSection(title = "Power Downloads") {
                SettingsRow(
                    title = "aria2 daemon status",
                    subtitle = if (daemonRunning) "Daemon active on localhost" else "Daemon stopped",
                    trailingText = if (daemonRunning) "Running" else "Stopped",
                    trailingTextColor = if (daemonRunning) Color(0xFF4CAF50) else Color(0xFFF44336),
                    showSwitch = true,
                    switchChecked = daemonRunning,
                    onSwitchChanged = { viewModel.toggleDaemon(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                SettingsRow(
                    title = "Global speed limit",
                    subtitle = "Applies to all background downloads",
                    trailingText = if (globalSpeedLimit == null || globalSpeedLimit!! <= 0) "Unlimited" else "${FormatUtils.formatSpeed(globalSpeedLimit!!)}",
                    showChevron = true,
                    onClick = { showSpeedDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                SettingsRow(
                    title = "Proxy",
                    subtitle = if (proxyConfig.enabled) "${proxyConfig.host}:${proxyConfig.port}" else "Disabled",
                    trailingText = if (proxyConfig.enabled) "On" else "Off",
                    trailingTextColor = if (proxyConfig.enabled) BrandPrimary else TextSecondary,
                    showChevron = true,
                    onClick = { showProxyDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                SettingsRow(
                    title = "Custom headers",
                    subtitle = "${customHeaders.size} headers configured",
                    showChevron = true,
                    onClick = { showHeadersDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                SettingsRow(
                    title = "Import cookies",
                    subtitle = "Load Netscape format cookies.txt for auth",
                    showChevron = true,
                    onClick = {
                        cookiePickerLauncher.launch(arrayOf("text/plain", "*/*"))
                    }
                )
            }

            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            // Schedule Section
            SettingsSection(title = "Schedule") {
                SettingsRow(
                    title = "Enable scheduling",
                    subtitle = "Automatically pause/resume downloads based on time",
                    showSwitch = true,
                    switchChecked = scheduleConfig.enabled,
                    onSwitchChanged = {
                        viewModel.saveSchedule(scheduleConfig.copy(enabled = it))
                    }
                )

                if (scheduleConfig.enabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                    val timeLabel = String.format("%02d:%02d – %02d:%02d", scheduleConfig.startHour, scheduleConfig.startMinute, scheduleConfig.endHour, scheduleConfig.endMinute)
                    SettingsRow(
                        title = "Active hours",
                        subtitle = "Downloads will only run during this window",
                        trailingText = timeLabel,
                        showChevron = true,
                        onClick = { showTimeDialog = true }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                    val daysLabel = formatDays(scheduleConfig.daysOfWeek)
                    SettingsRow(
                        title = "Active days",
                        subtitle = daysLabel,
                        showChevron = true,
                        onClick = { showDaysDialog = true }
                    )
                }
            }

            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            // BitTorrent Section
            SettingsSection(title = "BitTorrent") {
                SettingsRow(
                    title = "Listen port range",
                    subtitle = "Incoming peer connection ports",
                    trailingText = "6881–6999"
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                SettingsRow(
                    title = "Max peers per torrent",
                    subtitle = "Maximum connected peers",
                    trailingText = maxPeers.toString(),
                    showChevron = true,
                    onClick = { showPeersDialog = true }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                SettingsRow(
                    title = "Seed time (minutes)",
                    subtitle = "0 means stop immediately after download",
                    trailingText = "${seedTime} min",
                    showChevron = true,
                    onClick = { showSeedTimeDialog = true }
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.bit_torrent_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            // Accessibility Section
            SettingsSection(title = "Accessibility") {
                SettingsRow(
                    title = stringResource(R.string.reduce_motion_title),
                    subtitle = stringResource(R.string.reduce_motion_desc),
                    showSwitch = true,
                    switchChecked = reduceMotion,
                    onSwitchChanged = { viewModel.toggleReduceMotion(it) }
                )
            }

            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            // Diagnostics Section
            SettingsSection(title = "Diagnostics") {
                SettingsRow(
                    title = stringResource(R.string.view_crash_logs_title),
                    subtitle = stringResource(R.string.view_crash_logs_desc),
                    onClick = { showCrashLogsDialog = true }
                )
            }

            HorizontalDivider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant)

            // About Section
            SettingsSection(title = "About") {
                SettingsRow(
                    title = "BetterTube version",
                    trailingText = BuildConfig.VERSION_NAME
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                SettingsRow(
                    title = "aria2 version",
                    trailingText = aria2Version
                )

                if (aria2Features.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

                    SettingsRow(
                        title = "Enabled features",
                        subtitle = aria2Features.joinToString(", ")
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showCrashLogsDialog) {
        CrashLogsDialog(
            onDismiss = { showCrashLogsDialog = false }
        )
    }

    // Dialogs
    if (showSpeedDialog) {
        SpeedLimitDialog(
            currentLimit = globalSpeedLimit,
            onDismiss = { showSpeedDialog = false },
            onSave = {
                viewModel.setGlobalSpeedLimit(it)
                showSpeedDialog = false
            }
        )
    }

    if (showProxyDialog) {
        ProxyConfigDialog(
            initialConfig = proxyConfig,
            onDismiss = { showProxyDialog = false },
            onSave = {
                viewModel.saveProxy(it)
                showProxyDialog = false
            }
        )
    }

    if (showHeadersDialog) {
        HeadersDialog(
            initialHeaders = customHeaders,
            onDismiss = { showHeadersDialog = false },
            onSave = {
                viewModel.saveHeaders(it)
                showHeadersDialog = false
            },
            onValidationError = {
                viewModel.showSnackbar(it)
            }
        )
    }

    if (showTimeDialog) {
        ScheduleTimeDialog(
            startHour = scheduleConfig.startHour,
            startMinute = scheduleConfig.startMinute,
            endHour = scheduleConfig.endHour,
            endMinute = scheduleConfig.endMinute,
            onDismiss = { showTimeDialog = false },
            onSave = { sH, sM, eH, eM ->
                viewModel.saveSchedule(
                    scheduleConfig.copy(
                        startHour = sH,
                        startMinute = sM,
                        endHour = eH,
                        endMinute = eM
                    )
                )
                showTimeDialog = false
            }
        )
    }

    if (showDaysDialog) {
        ScheduleDaysDialog(
            currentDays = scheduleConfig.daysOfWeek,
            onDismiss = { showDaysDialog = false },
            onSave = { days ->
                viewModel.saveSchedule(scheduleConfig.copy(daysOfWeek = days))
                showDaysDialog = false
            }
        )
    }

    if (showPeersDialog) {
        NumericInputDialog(
            title = "Max Peers per Torrent",
            label = "Peer count (e.g. 128)",
            initialValue = maxPeers,
            onDismiss = { showPeersDialog = false },
            onSave = {
                viewModel.setMaxPeers(it)
                showPeersDialog = false
            }
        )
    }

    if (showSeedTimeDialog) {
        NumericInputDialog(
            title = "Seed Time (Minutes)",
            label = "Minutes to seed (0 = no seeding)",
            initialValue = seedTime,
            onDismiss = { showSeedTimeDialog = false },
            onSave = {
                viewModel.setSeedTime(it)
                showSeedTimeDialog = false
            }
        )
    }
}

private fun formatDays(days: Set<Int>): String {
    if (days.size == 7) return "Every day"
    val dayNames = mapOf(
        Calendar.SUNDAY to "Sun",
        Calendar.MONDAY to "Mon",
        Calendar.TUESDAY to "Tue",
        Calendar.WEDNESDAY to "Wed",
        Calendar.THURSDAY to "Thu",
        Calendar.FRIDAY to "Fri",
        Calendar.SATURDAY to "Sat"
    )
    return days.sorted().mapNotNull { dayNames[it] }.joinToString(", ")
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    BetterTubeTheme {
        SettingsScreen()
    }
}
