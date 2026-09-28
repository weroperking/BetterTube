package com.bettertube.app.ui.screens.home

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bettertube.app.BetterTubeApp
import com.bettertube.app.data.clipboard.ClipboardManager
import com.bettertube.app.domain.model.MediaFormat
import com.bettertube.app.domain.model.MediaMetadata
import com.bettertube.app.domain.usecase.FetchMetadataUseCase
import com.bettertube.app.domain.usecase.ObserveDownloadsUseCase
import com.bettertube.app.domain.usecase.StartDownloadUseCase
import com.bettertube.app.ui.screens.home.model.Platform
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val fetchMetadataUseCase: FetchMetadataUseCase,
    private val startDownloadUseCase: StartDownloadUseCase,
    private val observeDownloadsUseCase: ObserveDownloadsUseCase,
    private val clipboardManager: ClipboardManager? = null,
    @ApplicationContext private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private val _selectedFilter = MutableStateFlow("For You")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    fun onFilterSelected(filter: String) {
        _selectedFilter.value = filter
    }

    fun onPlatformClicked(platformName: String) {
        /* TODO Phase 4: open platform browser */
    }

    fun getPlatformUrl(platform: Platform): String {
        return if (platform.name.equals("More", ignoreCase = true) || platform.urlScheme.isBlank()) {
            "https://www.google.com"
        } else {
            "https://" + platform.urlScheme
        }
    }

    fun checkClipboardOnLaunch(): String? {
        val cm = clipboardManager ?: return null
        val lastDismissed = savedStateHandle.get<String>("lastDismissedUrl")
        return cm.hasNewUrl(lastDismissed)
    }

    fun onClipboardDismissed(url: String) {
        savedStateHandle["lastDismissedUrl"] = url
    }

    fun onRecommendedClicked(itemId: String) {
        /* TODO Phase 4 */
    }

    fun onTrendingClicked(itemId: String) {
        /* TODO Phase 4 */
    }

    private var currentMetadata: MediaMetadata? = null

    fun onUrlSubmitted(url: String) {
        // Edge Case 1: Check engine ready
        if (!BetterTubeApp.isEngineReady()) {
            _uiState.value = HomeUiState.Error("Engine not ready. Please restart the app.")
            return
        }

        // Edge Case 2: Validate URL format
        val trimmedUrl = url.trim()
        if (trimmedUrl.isBlank() || (!trimmedUrl.startsWith("http://", ignoreCase = true) && !trimmedUrl.startsWith("https://", ignoreCase = true))) {
            _uiState.value = HomeUiState.Error("Invalid URL")
            return
        }

        // Edge Case 4: Network connectivity check
        if (!isNetworkAvailable()) {
            _uiState.value = HomeUiState.Error("No internet connection")
            return
        }

        _uiState.value = HomeUiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            fetchMetadataUseCase(trimmedUrl).collect { result ->
                result.fold(
                    onSuccess = { metadata ->
                        // Edge Case 3: Empty formats check
                        if (metadata.formats.isEmpty()) {
                            _uiState.value = HomeUiState.Error("No downloadable formats found")
                        } else {
                            currentMetadata = metadata
                            _uiState.value = HomeUiState.MetadataLoaded(metadata)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = HomeUiState.Error(error.message ?: "Failed to extract video information")
                    }
                )
            }
        }
    }

    fun onQualitySelected(format: MediaFormat) {
        val metadata = currentMetadata ?: return
        viewModelScope.launch(Dispatchers.IO) {
            startDownloadUseCase(metadata.webpageUrl, metadata, format).collect { result ->
                result.fold(
                    onSuccess = { taskId ->
                        _uiState.value = HomeUiState.DownloadStarted(taskId)
                    },
                    onFailure = { error ->
                        _uiState.value = HomeUiState.Error(error.message ?: "Failed to start download")
                    }
                )
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val ctx = context ?: return true
        val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
