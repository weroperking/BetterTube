package com.bettertube.app.ui.screens.home

import com.bettertube.app.domain.model.MediaMetadata

sealed interface HomeUiState {
    data object Idle : HomeUiState
    data object Loading : HomeUiState
    data class MetadataLoaded(val metadata: MediaMetadata) : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class DownloadStarted(val taskId: String) : HomeUiState
}
