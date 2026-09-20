package com.bettertube.app.ui.screens.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.bettertube.app.domain.model.MediaMetadata
import com.bettertube.app.ui.components.RecommendedCardShimmer
import com.bettertube.app.ui.components.TrendingRowShimmer
import com.bettertube.app.ui.components.pressScale
import com.bettertube.app.ui.screens.home.components.FilterChipRow
import com.bettertube.app.ui.screens.home.components.HomeSearchBar
import com.bettertube.app.ui.screens.home.components.HomeTopBar
import com.bettertube.app.ui.screens.home.components.PlatformGrid
import com.bettertube.app.ui.screens.home.components.QualityPickerDialog
import com.bettertube.app.ui.screens.home.components.RecommendedCarousel
import com.bettertube.app.ui.screens.home.components.SectionHeader
import com.bettertube.app.ui.screens.home.components.TrendingListItem
import com.bettertube.app.ui.screens.home.model.Platform
import com.bettertube.app.ui.screens.home.model.RecommendedItem
import com.bettertube.app.ui.screens.home.model.TrendingItem
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BrandPrimary
import com.bettertube.app.ui.utils.rememberHaptics
import com.bettertube.app.utils.DeviceStateUtils
import com.example.R
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    navController: NavController = rememberNavController(),
    sharedUrl: String? = null,
    onPlatformClicked: (Platform) -> Unit = {},
    onSearchBarClicked: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val haptics = rememberHaptics()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var clipboardDetectedUrl by remember { mutableStateOf<String?>(null) }
    var showLowStorageDialog by remember { mutableStateOf(false) }
    var pendingBatteryUrl by remember { mutableStateOf<String?>(null) }

    fun processUrl(url: String) {
        if (!DeviceStateUtils.hasAdequateStorage(context)) {
            showLowStorageDialog = true
            return
        }
        if (DeviceStateUtils.isPowerSaveMode(context)) {
            pendingBatteryUrl = url
            return
        }
        viewModel.onUrlSubmitted(url)
    }

    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            processUrl(sharedUrl)
        }
    }

    LaunchedEffect(Unit) {
        val detected = viewModel.checkClipboardOnLaunch()
        if (detected != null) {
            clipboardDetectedUrl = detected
        }
    }

    // Clipboard Dialog
    if (clipboardDetectedUrl != null) {
        val urlToDownload = clipboardDetectedUrl!!
        AlertDialog(
            onDismissRequest = {
                viewModel.onClipboardDismissed(urlToDownload)
                clipboardDetectedUrl = null
            },
            title = {
                Text(text = stringResource(R.string.download_this_link))
            },
            text = {
                Text(text = urlToDownload)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onClipboardDismissed(urlToDownload)
                        clipboardDetectedUrl = null
                        processUrl(urlToDownload)
                    },
                    modifier = Modifier.testTag("clipboard_dialog_download_button")
                ) {
                    Text(text = stringResource(R.string.download))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.onClipboardDismissed(urlToDownload)
                        clipboardDetectedUrl = null
                    },
                    modifier = Modifier.testTag("clipboard_dialog_ignore_button")
                ) {
                    Text(text = stringResource(R.string.ignore))
                }
            },
            modifier = Modifier.testTag("clipboard_detected_dialog")
        )
    }

    // Low storage alert dialog
    if (showLowStorageDialog) {
        AlertDialog(
            onDismissRequest = { showLowStorageDialog = false },
            title = { Text(text = stringResource(R.string.storage_low_title)) },
            text = { Text(text = stringResource(R.string.storage_low_desc)) },
            confirmButton = {
                TextButton(
                    onClick = { showLowStorageDialog = false },
                    modifier = Modifier.testTag("storage_low_ok_button")
                ) {
                    Text(text = "OK")
                }
            },
            modifier = Modifier.testTag("storage_low_dialog")
        )
    }

    // Battery saver warning dialog
    if (pendingBatteryUrl != null) {
        val url = pendingBatteryUrl!!
        AlertDialog(
            onDismissRequest = { pendingBatteryUrl = null },
            title = { Text(text = stringResource(R.string.battery_saver_title)) },
            text = { Text(text = stringResource(R.string.battery_saver_desc)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingBatteryUrl = null
                        viewModel.onUrlSubmitted(url)
                    },
                    modifier = Modifier.testTag("battery_saver_continue_button")
                ) {
                    Text(text = stringResource(R.string.continue_anyway))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingBatteryUrl = null },
                    modifier = Modifier.testTag("battery_saver_cancel_button")
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            },
            modifier = Modifier.testTag("battery_saver_dialog")
        )
    }

    // Quality picker when metadata is loaded
    if (uiState is HomeUiState.MetadataLoaded) {
        val metadata = (uiState as HomeUiState.MetadataLoaded).metadata
        QualityPickerDialog(
            metadata = metadata,
            onDismiss = {
                viewModel.onSearchQueryChange("")
            },
            onDownload = { format ->
                viewModel.onQualitySelected(format)
            }
        )
    }

    HomeContent(
        innerPadding = innerPadding,
        searchQuery = searchQuery,
        selectedFilter = selectedFilter,
        isLoading = uiState is HomeUiState.Loading,
        uiState = uiState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onFilterSelected = { filter ->
            haptics.click()
            viewModel.onFilterSelected(filter)
        },
        onPlatformClick = { platform ->
            haptics.click()
            viewModel.onPlatformClicked(platform.name)
            onPlatformClicked(platform)
        },
        onRecommendedClick = { viewModel.onRecommendedClicked(it.id) },
        onTrendingClick = { viewModel.onTrendingClicked(it.id) },
        onSearchTap = onSearchBarClicked,
        onTopBarSearchClick = onSearchBarClicked
    )
}

@Composable
fun HomeContent(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    searchQuery: String = "",
    selectedFilter: String = "For You",
    isLoading: Boolean = false,
    uiState: HomeUiState = HomeUiState.Idle,
    onSearchQueryChange: (String) -> Unit = {},
    onFilterSelected: (String) -> Unit = {},
    onPlatformClick: (Platform) -> Unit = {},
    onRecommendedClick: (RecommendedItem) -> Unit = {},
    onTrendingClick: (TrendingItem) -> Unit = {},
    onSearchTap: () -> Unit = {},
    onTopBarSearchClick: () -> Unit = {},
    onSeeAllRecommendedClick: () -> Unit = {},
    onSeeAllTrendingClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val haptics = rememberHaptics()

    LaunchedEffect(uiState) {
        when (uiState) {
            is HomeUiState.Error -> {
                haptics.error()
                snackbarHostState.showSnackbar(uiState.message)
            }
            is HomeUiState.DownloadStarted -> {
                haptics.confirm()
                snackbarHostState.showSnackbar("Download started!")
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("home_lazy_column")
        ) {
            // 1. Top Bar row
            item(key = "top_bar") {
                HomeTopBar(onSearchClick = onTopBarSearchClick)
            }

            // 2. Search Bar
            item(key = "search_bar") {
                HomeSearchBar(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    onTap = onSearchTap
                )
            }

            // 3. Platform Grid
            item(key = "platform_grid") {
                PlatformGrid(
                    platforms = HomeMockData.platforms,
                    onPlatformClick = onPlatformClick
                )
            }

            // 4. Filter Chips Row
            item(key = "filter_chips") {
                FilterChipRow(
                    selected = selectedFilter,
                    onSelect = onFilterSelected
                )
            }

            // 5. Section Header "Recommended"
            item(key = "header_recommended") {
                SectionHeader(
                    title = "Recommended",
                    onSeeAllClick = onSeeAllRecommendedClick
                )
            }

            // 6. Recommended Carousel or Shimmer
            item(key = "recommended_carousel") {
                if (isLoading) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 12.dp)
                            .testTag("recommended_shimmer_row")
                    ) {
                        items(3) {
                            RecommendedCardShimmer()
                        }
                    }
                } else {
                    RecommendedCarousel(
                        items = HomeMockData.recommended,
                        onItemClick = onRecommendedClick
                    )
                }
            }

            // 7. Section Header "Trending Now"
            item(key = "header_trending") {
                SectionHeader(
                    title = "Trending Now",
                    onSeeAllClick = onSeeAllTrendingClick
                )
            }

            // 8. Trending list items or Shimmer
            if (isLoading) {
                items(5) {
                    TrendingRowShimmer()
                }
            } else {
                items(HomeMockData.trending, key = { it.id }) { item ->
                    TrendingListItem(
                        item = item,
                        onClick = onTrendingClick
                    )
                }
            }

            // 9. Bottom Spacer
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Paste a URL in the search bar above")
                }
            },
            containerColor = BrandPrimary,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp)
                .pressScale()
                .testTag("home_fab_download")
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download",
                tint = Color.Black
            )
        }

        // SnackbarHost
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .testTag("home_snackbar_host")
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BetterTubeTheme {
        HomeContent()
    }
}
