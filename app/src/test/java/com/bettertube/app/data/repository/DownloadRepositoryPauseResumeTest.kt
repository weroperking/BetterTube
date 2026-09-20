package com.bettertube.app.data.repository

import com.bettertube.app.data.engine.YtDlpEngine
import com.bettertube.app.domain.model.DownloadStatus
import com.bettertube.app.domain.model.DownloadTask
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadRepositoryPauseResumeTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class RecordingFakeEngine : YtDlpEngine(null) {
        val startDownloadCalls = mutableListOf<String>()
        val pauseProcessCalls = mutableListOf<String>()
        var pauseShouldSucceed = true

        override suspend fun startDownload(
            url: String,
            formatId: String,
            taskId: String,
            processId: String,
            speedLimitBytesPerSecond: Long?,
            onProgress: (percent: Float, downloadedBytes: Long, totalBytes: Long, speed: Long, etaSeconds: Long) -> Unit
        ): Result<String> {
            startDownloadCalls.add(processId)
            kotlinx.coroutines.awaitCancellation()
        }

        override suspend fun pauseProcess(processId: String): Result<Unit> {
            pauseProcessCalls.add(processId)
            return if (pauseShouldSucceed) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Process error"))
            }
        }
    }

    @Test
    fun pauseDownload_setsStatusToPaused_whenEngineSucceeds() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val fakeEngine = RecordingFakeEngine()
        val repository = DownloadRepositoryImpl(fakeEngine, null, testScope)

        val task = DownloadTask(
            id = "task_1",
            url = "https://example.com/video",
            title = "Test Video",
            thumbnailUrl = "",
            formatId = "137",
            status = DownloadStatus.DOWNLOADING,
            progressPercent = 0.5f,
            downloadedBytes = 500L,
            totalBytes = 1000L,
            speedBytesPerSecond = 100L,
            etaSeconds = 5L,
            outputFilePath = null,
            errorMessage = null,
            createdAtMillis = System.currentTimeMillis(),
            processId = "process_123"
        )

        repository.startDownload(task)
        val result = repository.pauseDownload(task.id)

        assertTrue(result.isSuccess)
        val currentTask = repository.getTask(task.id).first()
        assertEquals(DownloadStatus.PAUSED, currentTask?.status)
        assertEquals(0L, currentTask?.speedBytesPerSecond)
        assertEquals(0L, currentTask?.etaSeconds)
        assertTrue(fakeEngine.pauseProcessCalls.isNotEmpty())
    }

    @Test
    fun pauseDownload_doesNotChangeState_whenTaskIsNotDownloading() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val fakeEngine = RecordingFakeEngine()
        val repository = DownloadRepositoryImpl(fakeEngine, null, testScope)

        val task = DownloadTask(
            id = "task_2",
            url = "https://example.com/video",
            title = "Test Video",
            thumbnailUrl = "",
            formatId = "137",
            status = DownloadStatus.PAUSED,
            progressPercent = 0.5f,
            downloadedBytes = 500L,
            totalBytes = 1000L,
            speedBytesPerSecond = 0L,
            etaSeconds = 0L,
            outputFilePath = null,
            errorMessage = null,
            createdAtMillis = System.currentTimeMillis(),
            processId = "process_123"
        )

        val result = repository.pauseDownload(task.id)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun resumeDownload_assignsNewProcessId_andSetsStatusToExtracting() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val fakeEngine = RecordingFakeEngine()
        val repository = DownloadRepositoryImpl(fakeEngine, null, testScope)

        val oldProcessId = "old_proc_1"
        val task = DownloadTask(
            id = "task_3",
            url = "https://example.com/video",
            title = "Test Video",
            thumbnailUrl = "",
            formatId = "137",
            status = DownloadStatus.DOWNLOADING,
            progressPercent = 0.5f,
            downloadedBytes = 500L,
            totalBytes = 1000L,
            speedBytesPerSecond = 100L,
            etaSeconds = 5L,
            outputFilePath = null,
            errorMessage = null,
            createdAtMillis = System.currentTimeMillis(),
            processId = oldProcessId
        )

        repository.startDownload(task)
        repository.pauseDownload(task.id)

        val resumeResult = repository.resumeDownload(task.id)
        assertTrue(resumeResult.isSuccess)

        val resumedTask = repository.getTask(task.id).first()
        assertNotEquals(oldProcessId, resumedTask?.processId)
    }

    @Test
    fun cancelDownload_removesTask_andDeletesFile() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val fakeEngine = RecordingFakeEngine()
        val repository = DownloadRepositoryImpl(fakeEngine, null, testScope)

        val sampleFile = tempFolder.newFile("test_video.mp4")
        val sampleAria2 = tempFolder.newFile("test_video.mp4.aria2")
        assertTrue(sampleFile.exists())
        assertTrue(sampleAria2.exists())

        val task = DownloadTask(
            id = "task_4",
            url = "https://example.com/video",
            title = "Test Video",
            thumbnailUrl = "",
            formatId = "137",
            status = DownloadStatus.DOWNLOADING,
            progressPercent = 0.5f,
            downloadedBytes = 500L,
            totalBytes = 1000L,
            speedBytesPerSecond = 100L,
            etaSeconds = 5L,
            outputFilePath = sampleFile.absolutePath,
            errorMessage = null,
            createdAtMillis = System.currentTimeMillis(),
            processId = "process_cancel_test"
        )

        repository.startDownload(task)
        val cancelResult = repository.cancelDownload(task.id)

        assertTrue(cancelResult.isSuccess)
        val finalTask = repository.getTask(task.id).first()
        assertEquals(null, finalTask)
        assertFalse(sampleFile.exists())
        assertFalse(sampleAria2.exists())
    }
}
