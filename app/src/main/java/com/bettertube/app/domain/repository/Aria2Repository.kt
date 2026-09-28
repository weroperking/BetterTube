package com.bettertube.app.domain.repository

import com.bettertube.app.domain.model.Aria2Download
import com.bettertube.app.domain.model.ProxyConfig
import com.bettertube.app.domain.model.ScheduleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Aria2Repository {
    fun isDaemonRunning(): StateFlow<Boolean>
    suspend fun startDaemon(): Result<Unit>
    suspend fun stopDaemon(): Result<Unit>
    suspend fun addMagnet(magnetUri: String, saveDir: String?): Result<String>
    suspend fun addTorrentFile(torrentBytes: ByteArray, saveDir: String?): Result<String>
    suspend fun addMetalinkFile(metalinkBytes: ByteArray): Result<List<String>>
    suspend fun addHttpDownload(url: String, saveDir: String?): Result<String>
    suspend fun pauseDownload(gid: String): Result<Unit>
    suspend fun resumeDownload(gid: String): Result<Unit>
    suspend fun removeDownload(gid: String): Result<Unit>
    suspend fun purgeCompleted(): Result<Unit>
    fun observeActiveDownloads(): Flow<List<Aria2Download>>
    suspend fun setGlobalSpeedLimit(bytesPerSecond: Long?): Result<Unit>
    fun getGlobalSpeedLimit(): StateFlow<Long?>
    suspend fun setProxy(config: ProxyConfig): Result<Unit>
    suspend fun setCustomHeaders(headers: Map<String, String>): Result<Unit>
    suspend fun setCookieFile(cookieContent: String): Result<Unit>
    fun getProxyConfig(): StateFlow<ProxyConfig>
    fun getScheduleConfig(): StateFlow<ScheduleConfig>
    suspend fun setScheduleConfig(config: ScheduleConfig): Result<Unit>
    suspend fun pauseAllActive(): Result<Unit>
    suspend fun resumeAllPaused(): Result<Unit>
    fun close()
    suspend fun getVersionInfo(): Pair<String, List<String>>?
    suspend fun setMaxPeers(peers: Int): Result<Unit>
    suspend fun setSeedTime(minutes: Int): Result<Unit>
}
