package com.bettertube.app.ui.screens.downloads

import android.os.SystemClock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bettertube.app.domain.model.Aria2Download
import com.bettertube.app.domain.model.DownloadStatus
import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.domain.model.MediaType
import com.bettertube.app.ui.components.DownloadItemShimmer
import com.bettertube.app.ui.navigation.Screen
import com.bettertube.app.ui.screens.downloads.components.AddPowerDownloadDialog
import com.bettertube.app.ui.screens.downloads.components.Aria2TaskCard
import com.bettertube.app.ui.screens.downloads.components.DownloadTaskCard
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BgSecondary
import com.bettertube.app.ui.theme.BrandPrimary
import com.bettertube.app.ui.theme.SurfaceDark
import com.bettertube.app.ui.theme.TextSecondary
import com.bettertube.app.ui.utils.rememberHaptics
import com.bettertube.app.R

@Composable
fun DownloadsScreen(
    navController: NavController? = null,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val aria2Downloads by viewModel.aria2Downloads.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddDialog by remember { mutableStateOf(false) }
    val haptics = rememberHaptics()

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        DownloadsContent(
            tasks = tasks,
            aria2Downloads = aria2Downloads,
            selectedFilter = selectedFilter,
            isLoading = isLoading,
            onFilterSelected = {
                haptics.click()
                viewModel.onFilterSelected(it)
            },
            onAddClick = { showAddDialog = true },
            onSettingsClick = {
                navController?.navigate(Screen.Settings.route)
            },
            onPause = {
                haptics.confirm()
                viewModel.onPause(it)
            },
            onResume = {
                haptics.confirm()
                viewModel.onResume(it)
            },
            onCancel = { viewModel.onCancel(it) },
            onRetry = { viewModel.onRetry(it) },
            onOpen = { viewModel.onOpen(it) },
            onMoveUp = { viewModel.onMoveUp(it) },
            onMoveDown = { viewModel.onMoveDown(it) },
            onPauseAria2 = {
                haptics.confirm()
                viewModel.onPauseAria2(it)
            },
            onResumeAria2 = {
                haptics.confirm()
                viewModel.onResumeAria2(it)
            },
            onRemoveAria2 = { viewModel.onRemoveAria2(it) },
            modifier = Modifier.padding(innerPadding)
        )
    }

    if (showAddDialog) {
        AddPowerDownloadDialog(
            onDismiss = { showAddDialog = false },
            onAddMagnet = { viewModel.onAddMagnet(it) },
            onAddHttpUrl = { viewModel.onAddHttpUrl(it) },
            onAddTorrentFile = { viewModel.onAddTorrentFile(it) },
            onAddMetalinkFile = { viewModel.onAddMetalinkFile(it) }
        )
    }
}

@Composable
fun DownloadsContent(
    tasks: List<DownloadTask>,
    aria2Downloads: List<Aria2Download> = emptyList(),
    selectedFilter: String,
    isLoading: Boolean = false,
    onFilterSelected: (String) -> Unit,
    onAddClick: () -> Unit = {},
    onSettingsClick: () -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRetry: (String) -> Unit,
    onOpen: (String) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onPauseAria2: (String) -> Unit = {},
    onResumeAria2: (String) -> Unit = {},
    onRemoveAria2: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val filters = listOf("All", "Video", "Audio", "Images")

    // Filter tasks
    val filteredTasks = when (selectedFilter) {
        "Video" -> tasks.filter { it.mediaType == MediaType.VIDEO }
        "Audio" -> tasks.filter { it.mediaType == MediaType.AUDIO }
        "Images" -> tasks.filter { it.mediaType == MediaType.IMAGE }
        else -> tasks
    }

    // Grouping
    val downloadingTasks = filteredTasks.filter {
        it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.EXTRACTING || it.status == DownloadStatus.MERGING
    }.sortedByDescending { it.createdAtMillis }

    val waitingTasks = filteredTasks.filter {
        it.status == DownloadStatus.WAITING
    }

    val pausedTasks = filteredTasks.filter {
        it.status == DownloadStatus.PAUSED
    }.sortedByDescending { it.createdAtMillis }

    val completedTasks = filteredTasks.filter {
        it.status == DownloadStatus.COMPLETE
    }.sortedByDescending { it.createdAtMillis }

    val failedTasks = filteredTasks.filter {
        it.status == DownloadStatus.FAILED || it.status == DownloadStatus.CANCELLED
    }.sortedByDescending { it.createdAtMillis }

    val isDark = isSystemInDarkTheme()
    val inactiveChipBg = if (isDark) SurfaceDark else BgSecondary
    val isEmpty = filteredTasks.isEmpty() && aria2Downloads.isEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.downloads_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onAddClick,
                        modifier = Modifier.testTag("downloads_add_power_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Download / Magnet",
                            tint = BrandPrimary
                        )
                    }

                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.testTag("downloads_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_cd),
                            tint = TextSecondary
                        )
                    }
                }
            }

            // Filter chips row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) BrandPrimary else inactiveChipBg)
                            .clickable { onFilterSelected(filter) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("filter_chip_${filter.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filter,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Color.Black else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main List or Empty state / Shimmer
            if (isLoading && isEmpty) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("downloads_shimmer_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(4) {
                        DownloadItemShimmer()
                    }
                }
            } else if (isEmpty) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("empty_downloads_view"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.no_downloads_yet),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("downloads_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // aria2 Power Downloads Section
                    if (aria2Downloads.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Power Downloads (aria2)")
                        }
                        items(aria2Downloads, key = { it.gid }) { aria2Download ->
                            Aria2TaskCard(
                                download = aria2Download,
                                onPause = onPauseAria2,
                                onResume = onResumeAria2,
                                onRemove = onRemoveAria2
                            )
                        }
                    }

                    if (downloadingTasks.isNotEmpty()) {
                        item {
                            SectionHeader(title = stringResource(R.string.section_downloading))
                        }
                        items(downloadingTasks, key = { it.id }) { task ->
                            DownloadTaskCard(
                                task = task,
                                onPause = { onPause(task.id) },
                                onResume = { onResume(task.id) },
                                onCancel = { onCancel(task.id) },
                                onRetry = { onRetry(task.id) },
                                onOpen = { onOpen(task.id) },
                                onMoveUp = null,
                                onMoveDown = null
                            )
                        }
                    }

                    if (waitingTasks.isNotEmpty()) {
                        item {
                            SectionHeader(title = stringResource(R.string.section_waiting))
                        }
                        itemsIndexed(waitingTasks, key = { _, task -> task.id }) { index, task ->
                            val canMoveUp = if (index > 0) { { onMoveUp(task.id) } } else null
                            val canMoveDown = if (index < waitingTasks.size - 1) { { onMoveDown(task.id) } } else null
                            DownloadTaskCard(
                                task = task,
                                onPause = { onPause(task.id) },
                                onResume = { onResume(task.id) },
                                onCancel = { onCancel(task.id) },
                                onRetry = { onRetry(task.id) },
                                onOpen = { onOpen(task.id) },
                                onMoveUp = canMoveUp,
                                onMoveDown = canMoveDown
                            )
                        }
                    }

                    if (pausedTasks.isNotEmpty()) {
                        item {
                            SectionHeader(title = stringResource(R.string.section_paused))
                        }
                        items(pausedTasks, key = { it.id }) { task ->
                            DownloadTaskCard(
                                task = task,
                                onPause = { onPause(task.id) },
                                onResume = { onResume(task.id) },
                                onCancel = { onCancel(task.id) },
                                onRetry = { onRetry(task.id) },
                                onOpen = { onOpen(task.id) },
                                onMoveUp = null,
                                onMoveDown = null
                            )
                        }
                    }

                    if (completedTasks.isNotEmpty()) {
                        item {
                            SectionHeader(title = stringResource(R.string.section_completed))
                        }
                        items(completedTasks, key = { it.id }) { task ->
                            DownloadTaskCard(
                                task = task,
                                onPause = { onPause(task.id) },
                                onResume = { onResume(task.id) },
                                onCancel = { onCancel(task.id) },
                                onRetry = { onRetry(task.id) },
                                onOpen = { onOpen(task.id) },
                                onMoveUp = null,
                                onMoveDown = null
                            )
                        }
                    }

                    if (failedTasks.isNotEmpty()) {
                        item {
                            SectionHeader(title = stringResource(R.string.section_failed))
                        }
                        items(failedTasks, key = { it.id }) { task ->
                            DownloadTaskCard(
                                task = task,
                                onPause = { onPause(task.id) },
                                onResume = { onResume(task.id) },
                                onCancel = { onCancel(task.id) },
                                onRetry = { onRetry(task.id) },
                                onOpen = { onOpen(task.id) },
                                onMoveUp = null,
                                onMoveDown = null
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics { heading() }
            .testTag("section_header_${title.lowercase().replace(" ", "_")}")
    )
}

@Preview(showBackground = true)
@Composable
fun DownloadsScreenPreview() {
    BetterTubeTheme {
        DownloadsContent(
            tasks = listOf(
                DownloadTask(
                    id = "task_preview_1",
                    url = "https://example.com/video1",
                    title = "Sample Active Download Video",
                    thumbnailUrl = "",
                    formatId = "137",
                    status = DownloadStatus.DOWNLOADING,
                    progressPercent = 0.5f,
                    downloadedBytes = 50000000L,
                    totalBytes = 100000000L,
                    speedBytesPerSecond = 2048000L,
                    etaSeconds = 25L,
                    outputFilePath = null,
                    errorMessage = null,
                    createdAtMillis = SystemClock.elapsedRealtime(),
                    mediaType = MediaType.VIDEO
                )
            ),
            aria2Downloads = emptyList(),
            selectedFilter = "All",
            onFilterSelected = {},
            onAddClick = {},
            onSettingsClick = {},
            onPause = {},
            onResume = {},
            onCancel = {},
            onRetry = {},
            onOpen = {},
            onMoveUp = {},
            onMoveDown = {}
        )
    }
}
