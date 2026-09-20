package com.bettertube.app.data.repository

import com.bettertube.app.data.engine.YtDlpEngine
import com.bettertube.app.domain.model.DownloadStatus
import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.utils.NetworkMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadRepositoryQueueTest {

    private class StubNetworkMonitor(private val onWifi: Boolean) : NetworkMonitor(true) {
        override fun isOnWifi(): Boolean = onWifi
        override fun isConnected(): Boolean = true
    }

    private class NoOpEngine : YtDlpEngine(null)

    @Test
    fun reorderQueue_updatesOrder_whenAllIdsAreWaiting() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val networkMonitor = StubNetworkMonitor(onWifi = false)
        val repository = DownloadRepositoryImpl(NoOpEngine(), null, testScope, networkMonitor)

        repository.setWifiOnly(true)

        val task1 = DownloadTask(
            id = "wait_1",
            url = "https://example.com/1",
            title = "Task 1",
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

        val task2 = DownloadTask(
            id = "wait_2",
            url = "https://example.com/2",
            title = "Task 2",
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
            createdAtMillis = 2000L
        )

        repository.startDownload(task1)
        repository.startDownload(task2)

        val reorderResult = repository.reorderQueue(listOf("wait_2", "wait_1"))
        assertTrue(reorderResult.isSuccess)
    }

    @Test
    fun reorderQueue_returnsFailure_whenUnknownIdProvided() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val repository = DownloadRepositoryImpl(NoOpEngine(), null, testScope)

        val result = repository.reorderQueue(listOf("non_existent_id"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun reorderQueue_returnsFailure_whenDuplicateIdsProvided() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val networkMonitor = StubNetworkMonitor(onWifi = false)
        val repository = DownloadRepositoryImpl(NoOpEngine(), null, testScope, networkMonitor)

        repository.setWifiOnly(true)

        val task = DownloadTask(
            id = "wait_dup",
            url = "https://example.com/dup",
            title = "Task Dup",
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
        repository.startDownload(task)

        val result = repository.reorderQueue(listOf("wait_dup", "wait_dup"))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }
}
