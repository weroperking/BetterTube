package com.bettertube.app.data.repository

import com.bettertube.app.data.engine.YtDlpEngine
import com.bettertube.app.domain.model.DownloadStatus
import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.utils.NetworkMonitor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadRepositoryWifiTest {

    private class StubNetworkMonitor(var onWifi: Boolean) : NetworkMonitor(true) {
        override fun isOnWifi(): Boolean = onWifi
        override fun isConnected(): Boolean = true
    }

    private class FakeEngine : YtDlpEngine(null) {
        var startDownloadCalled = false

        override suspend fun startDownload(
            url: String,
            formatId: String,
            taskId: String,
            processId: String,
            speedLimitBytesPerSecond: Long?,
            onProgress: (percent: Float, downloadedBytes: Long, totalBytes: Long, speed: Long, etaSeconds: Long) -> Unit
        ): Result<String> {
            startDownloadCalled = true
            kotlinx.coroutines.awaitCancellation()
        }
    }

    @Test
    fun startDownload_setsWaiting_whenWifiOnlyEnabledAndOnCellular() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val networkMonitor = StubNetworkMonitor(onWifi = false)
        val fakeEngine = FakeEngine()
        val repository = DownloadRepositoryImpl(fakeEngine, null, testScope, networkMonitor)

        repository.setWifiOnly(true)

        val task = DownloadTask(
            id = "cellular_task",
            url = "https://example.com/cell",
            title = "Cellular Task",
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

        val savedTask = repository.getTask(task.id).first()
        assertEquals(DownloadStatus.WAITING, savedTask?.status)
        assertEquals(false, fakeEngine.startDownloadCalled)
    }

    @Test
    fun startDownload_proceeds_whenWifiOnlyEnabledAndOnWifi() = runTest {
        val testDispatcher = UnconfinedTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val networkMonitor = StubNetworkMonitor(onWifi = true)
        val fakeEngine = FakeEngine()
        val repository = DownloadRepositoryImpl(fakeEngine, null, testScope, networkMonitor)

        repository.setWifiOnly(true)

        val task = DownloadTask(
            id = "wifi_task",
            url = "https://example.com/wifi",
            title = "Wifi Task",
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

        val savedTask = repository.getTask(task.id).first()
        assertEquals(DownloadStatus.DOWNLOADING, savedTask?.status)
        assertEquals(true, fakeEngine.startDownloadCalled)
    }
}
