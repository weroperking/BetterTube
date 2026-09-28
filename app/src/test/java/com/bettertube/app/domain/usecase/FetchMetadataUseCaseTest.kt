package com.bettertube.app.domain.usecase

import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.domain.model.MediaMetadata
import com.bettertube.app.domain.repository.DownloadRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FetchMetadataUseCaseTest {

    private class FakeDownloadRepository(
        private val expectedResult: Result<MediaMetadata>
    ) : DownloadRepository {
        override suspend fun fetchMetadata(url: String): Result<MediaMetadata> = expectedResult
        override suspend fun fetchPlaylist(url: String): Result<com.bettertube.app.domain.model.PlaylistInfo> = Result.failure(Exception("Not implemented"))
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

    @Test
    fun fetchMetadataUseCase_emitsSameResultAsRepository() = runTest(UnconfinedTestDispatcher()) {
        val cannedMetadata = MediaMetadata(
            id = "vid_abc",
            title = "Sample Video",
            uploader = "Sample Creator",
            durationSeconds = 120L,
            thumbnailUrl = "https://example.com/thumb.png",
            webpageUrl = "https://example.com/video",
            formats = emptyList(),
            extractor = "generic"
        )
        val expectedResult = Result.success(cannedMetadata)
        val fakeRepo = FakeDownloadRepository(expectedResult)
        val useCase = FetchMetadataUseCase(fakeRepo)

        val flowResult = useCase("https://example.com/video").first()

        assertTrue(flowResult.isSuccess)
        assertEquals("Sample Video", flowResult.getOrNull()?.title)
        assertEquals(expectedResult, flowResult)
    }
}
