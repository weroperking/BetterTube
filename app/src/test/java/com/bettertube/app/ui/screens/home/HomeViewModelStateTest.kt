package com.bettertube.app.ui.screens.home

import androidx.lifecycle.SavedStateHandle
import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.domain.model.MediaMetadata
import com.bettertube.app.domain.repository.DownloadRepository
import com.bettertube.app.domain.usecase.FetchMetadataUseCase
import com.bettertube.app.domain.usecase.ObserveDownloadsUseCase
import com.bettertube.app.domain.usecase.StartDownloadUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HomeViewModelStateTest {

    private class FakeRepository : DownloadRepository {
        override suspend fun fetchMetadata(url: String): Result<MediaMetadata> = Result.failure(Exception("stub"))
        override suspend fun fetchPlaylist(url: String): Result<com.bettertube.app.domain.model.PlaylistInfo> = Result.failure(Exception("stub"))
        override suspend fun startPlaylistDownload(
            url: String,
            selectedIndices: List<Int>,
            formatId: String,
            preset: com.bettertube.app.domain.model.ExtractionPreset,
            downloadSubtitles: Boolean,
            subtitleLanguages: List<String>,
            embedSubtitles: Boolean
        ): Result<List<String>> = Result.success(emptyList())
        override suspend fun fetchSubtitles(url: String): Result<List<com.bettertube.app.domain.model.SubtitleTrack>> = Result.success(emptyList())
        override suspend fun startDownload(task: DownloadTask): Result<String> = Result.success(task.id)
        override suspend fun pauseDownload(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun resumeDownload(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun cancelDownload(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun retryDownload(id: String): Result<Unit> = Result.success(Unit)
        override suspend fun setDownloadSpeedLimit(id: String, bytesPerSecond: Long?): Result<Unit> = Result.success(Unit)
        override suspend fun setGlobalSpeedLimit(bytesPerSecond: Long?): Result<Unit> = Result.success(Unit)
        override suspend fun reorderQueue(orderedIds: List<String>): Result<Unit> = Result.success(Unit)
        override fun getGlobalSpeedLimit(): StateFlow<Long?> = MutableStateFlow(null)
        override fun getWifiOnly(): StateFlow<Boolean> = MutableStateFlow(false)
        override suspend fun setWifiOnly(enabled: Boolean): Result<Unit> = Result.success(Unit)
        override fun getAllTasks(): Flow<List<DownloadTask>> = emptyFlow()
        override fun getTask(id: String): Flow<DownloadTask?> = emptyFlow()
    }

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        val repo = FakeRepository()
        viewModel = HomeViewModel(
            savedStateHandle = SavedStateHandle(),
            fetchMetadataUseCase = FetchMetadataUseCase(repo),
            startDownloadUseCase = StartDownloadUseCase(repo),
            observeDownloadsUseCase = ObserveDownloadsUseCase(repo),
            context = null
        )
    }

    @Test
    fun onSearchQueryChange_updatesSearchQueryState() {
        val query = "https://youtube.com/watch?v=123"
        viewModel.onSearchQueryChange(query)
        assertEquals(query, viewModel.searchQuery.value)
    }

    @Test
    fun onFilterSelected_updatesSelectedFilterState() {
        val filter = "Music"
        viewModel.onFilterSelected(filter)
        assertEquals(filter, viewModel.selectedFilter.value)
    }

    @Test
    fun initialSelectedFilter_isForYou() {
        assertEquals("For You", viewModel.selectedFilter.value)
    }
}
