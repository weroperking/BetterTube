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
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadRepositorySpeedLimitTest {

    private class RecordingEngine : YtDlpEngine(null) {
        val pauseCalls = mutableListOf<String>()
        val startCalls = mutableListOf<String>()

        override suspend fun startDownload(
            url: String,
            formatId: String,
            taskId: String,
            processId: String,
            speedLimitBytesPerSecond: Long?,
            onProgress: (percent: Float, downloadedBytes: Long, totalBytes: Long, speed: Long, etaSeconds: Long) -> Unit
        ): Result<String> {
            startCalls.add(processId)
            kotlinx.coroutines.awaitCancellation()
        }

        override suspend fun pauseProcess(processId: String): Result<Unit> {
            pauseCalls.add(processId)
            return Result.success(Unit)
        }
    }

    @Test
    fun setGlobalSpeedLimit_updatesStateFlow() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val engine = RecordingEngine()
        val repository = DownloadRepositoryImpl(engine, null, testScope)

        val result = repository.setGlobalSpeedLimit(1024L * 1024L)
        assertTrue(result.isSuccess)
        assertEquals(1024L * 1024L, repository.getGlobalSpeedLimit().value)

        // Clear limit with 0 or negative
        repository.setGlobalSpeedLimit(0L)
        assertEquals(null, repository.getGlobalSpeedLimit().value)
    }

    @Test
    fun setDownloadSpeedLimit_pausesAndResumes_whenTaskIsActive() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val engine = RecordingEngine()
        val repository = DownloadRepositoryImpl(engine, null, testScope)

        val task = DownloadTask(
            id = "speed_task",
            url = "https://example.com/video",
            title = "Speed Test Video",
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
            createdAtMillis = 1000L,
            processId = "speed_proc_1"
        )

        repository.startDownload(task)
        val initialPauseCalls = engine.pauseCalls.size
        val initialStartCalls = engine.startCalls.size

        val limitResult = repository.setDownloadSpeedLimit(task.id, 512L * 1024L)
        assertTrue(limitResult.isSuccess)

        val updatedTask = repository.getTask(task.id).first()
        assertEquals(512L * 1024L, updatedTask?.speedLimitBytesPerSecond)
        // Verify pause and restart were triggered
        assertTrue(engine.pauseCalls.size > initialPauseCalls)
        assertTrue(engine.startCalls.size > initialStartCalls)
    }
}
