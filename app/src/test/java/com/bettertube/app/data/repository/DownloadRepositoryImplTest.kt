package com.bettertube.app.data.repository

import com.bettertube.app.data.engine.EngineException
import com.bettertube.app.data.engine.YtDlpEngine
import com.bettertube.app.domain.model.DownloadStatus
import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.domain.model.MediaFormat
import com.bettertube.app.domain.model.MediaMetadata
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadRepositoryImplTest {

    private class FakeYtDlpEngine(
        var shouldFail: Boolean = false,
        var exceptionToThrow: Throwable = EngineException.ExtractionFailed("Simulated extraction error")
    ) : YtDlpEngine(null) {

        val cannedMetadata = MediaMetadata(
            id = "test_id_123",
            title = "Test Video Title",
            uploader = "Test Channel",
            durationSeconds = 180L,
            thumbnailUrl = "https://example.com/thumb.jpg",
            webpageUrl = "https://example.com/watch?v=123",
            formats = listOf(
                MediaFormat(
                    formatId = "137",
                    extension = "mp4",
                    resolution = "1080p",
                    audioBitrate = null,
                    videoBitrate = 4500,
                    fileSizeBytes = 50_000_000L,
                    note = "1080p video",
                    isVideo = true,
                    isAudio = false
                )
            ),
            extractor = "youtube"
        )

        override suspend fun extractInfo(url: String): Result<MediaMetadata> {
            return if (shouldFail) {
                Result.failure(exceptionToThrow)
            } else {
                Result.success(cannedMetadata)
            }
        }

        override suspend fun startDownload(
            url: String,
            formatId: String,
            taskId: String,
            processId: String,
            speedLimitBytesPerSecond: Long?,
            onProgress: (percent: Float, downloadedBytes: Long, totalBytes: Long, speed: Long, etaSeconds: Long) -> Unit
        ): Result<String> {
            if (shouldFail) {
                return Result.failure(EngineException.DownloadFailed("Simulated download error"))
            }
            // Report progress 3 times: 0.0, 0.5, 1.0
            onProgress(0.0f, 0L, 1000L, 500L, 2L)
            onProgress(0.5f, 500L, 1000L, 500L, 1L)
            onProgress(1.0f, 1000L, 1000L, 500L, 0L)
            return Result.success(taskId)
        }

        override suspend fun cancelDownload(): Result<Unit> {
            return Result.success(Unit)
        }
    }

    @Test
    fun fetchMetadata_returnsSuccess_withCorrectTitle_whenEngineSucceeds() = runTest {
        val fakeEngine = FakeYtDlpEngine(shouldFail = false)
        val repository = DownloadRepositoryImpl(fakeEngine, null, this)

        val result = repository.fetchMetadata("https://www.youtube.com/watch?v=123")

        assertTrue(result.isSuccess)
        assertEquals("Test Video Title", result.getOrNull()?.title)
        assertEquals("test_id_123", result.getOrNull()?.id)
    }

    @Test
    fun fetchMetadata_returnsFailure_whenEngineThrows() = runTest {
        val fakeEngine = FakeYtDlpEngine(shouldFail = true)
        val repository = DownloadRepositoryImpl(fakeEngine, null, this)

        val result = repository.fetchMetadata("https://www.youtube.com/watch?v=123")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is EngineException.ExtractionFailed)
    }

    @Test
    fun startDownload_updatesTaskList_fromDownloadingToComplete_andRecordsPercentOne() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val fakeEngine = FakeYtDlpEngine(shouldFail = false)
        val repository = DownloadRepositoryImpl(fakeEngine, null, testScope)

        val task = DownloadTask(
            id = UUID.randomUUID().toString(),
            url = "https://www.youtube.com/watch?v=123",
            title = "Test Video Title",
            thumbnailUrl = "https://example.com/thumb.jpg",
            formatId = "137",
            status = DownloadStatus.QUEUED,
            progressPercent = 0.0f,
            downloadedBytes = 0L,
            totalBytes = 1000L,
            speedBytesPerSecond = 0L,
            etaSeconds = 0L,
            outputFilePath = null,
            errorMessage = null,
            createdAtMillis = System.currentTimeMillis()
        )

        val startResult = repository.startDownload(task)
        assertTrue(startResult.isSuccess)

        val tasks = repository.getAllTasks().first()
        val completedTask = tasks.firstOrNull { it.id == task.id }

        assertTrue(completedTask != null)
        assertEquals(DownloadStatus.COMPLETE, completedTask?.status)
        assertEquals(1.0f, completedTask?.progressPercent ?: 0f, 0.001f)
    }

    @Test
    fun getAllTasks_returnsTasksSortedByCreatedAtMillisDescending() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val fakeEngine = FakeYtDlpEngine(shouldFail = false)
        val repository = DownloadRepositoryImpl(fakeEngine, null, testScope)

        val olderTask = DownloadTask(
            id = "task_older",
            url = "https://example.com/older",
            title = "Older Video",
            thumbnailUrl = "",
            formatId = "137",
            status = DownloadStatus.QUEUED,
            progressPercent = 0f,
            downloadedBytes = 0L,
            totalBytes = 0L,
            speedBytesPerSecond = 0L,
            etaSeconds = 0L,
            outputFilePath = null,
            errorMessage = null,
            createdAtMillis = 1000L
        )

        val newerTask = DownloadTask(
            id = "task_newer",
            url = "https://example.com/newer",
            title = "Newer Video",
            thumbnailUrl = "",
            formatId = "137",
            status = DownloadStatus.QUEUED,
            progressPercent = 0f,
            downloadedBytes = 0L,
            totalBytes = 0L,
            speedBytesPerSecond = 0L,
            etaSeconds = 0L,
            outputFilePath = null,
            errorMessage = null,
            createdAtMillis = 5000L
        )

        repository.startDownload(olderTask)
        repository.startDownload(newerTask)

        val allTasks = repository.getAllTasks().first()
        assertEquals(2, allTasks.size)
        assertEquals("task_newer", allTasks[0].id)
        assertEquals("task_older", allTasks[1].id)
    }
}
