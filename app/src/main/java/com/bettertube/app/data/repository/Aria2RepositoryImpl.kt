package com.bettertube.app.data.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.bettertube.app.data.aria2.Aria2ProcessManager
import com.bettertube.app.data.aria2.rpc.Aria2RpcClient
import com.bettertube.app.data.aria2.rpc.Aria2Status
import com.bettertube.app.data.schedule.ScheduleConfigStore
import com.bettertube.app.domain.model.Aria2Download
import com.bettertube.app.domain.model.Aria2DownloadStatus
import com.bettertube.app.domain.model.ProxyConfig
import com.bettertube.app.domain.model.ScheduleConfig
import com.bettertube.app.domain.repository.Aria2Repository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Aria2RepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val rpcClient: Aria2RpcClient,
    private val processManager: Aria2ProcessManager,
    private val scheduleConfigStore: ScheduleConfigStore
) : Aria2Repository, AutoCloseable {

    companion object {
        private const val TAG = "Aria2RepositoryImpl"
        private const val KEY_PROXY = "proxy"
        private const val KEY_HEADERS = "custom_headers"
        private const val KEY_SPEED_LIMIT = "aria2_global_speed_limit"
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val settingsFile = File(context.filesDir, "settings.json")

    private val _proxyConfig = MutableStateFlow(loadProxyConfig())
    private val _globalSpeedLimit = MutableStateFlow(loadGlobalSpeedLimit())

    init {
        scope.launch {
            try {
                // Edge Case 3: Check if daemon is already running (orphan process)
                val version = rpcClient.getVersion()
                if (version != null) {
                    Log.i(TAG, "Reusing existing aria2 daemon: ${version.version}")
                    processManager.markAsRunning(processManager.getRpcPort())
                } else {
                    val startRes = processManager.start()
                    if (startRes.isFailure) {
                        Log.w(TAG, "aria2 daemon failed to start on init: ${startRes.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error initializing aria2 daemon", e)
            }
        }
    }

    private fun defaultDir(): String {
        val dir = File(context.getExternalFilesDir(null), "BetterTube")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    override fun isDaemonRunning(): StateFlow<Boolean> = processManager.isRunning

    override suspend fun startDaemon(): Result<Unit> = processManager.start()

    override suspend fun stopDaemon(): Result<Unit> = processManager.stop()

    override suspend fun addMagnet(magnetUri: String, saveDir: String?): Result<String> {
        val options = mapOf(
            "dir" to (saveDir ?: defaultDir()),
            "seed-time" to "0",
            "bt-stop-timeout" to "300"
        )
        val gid = rpcClient.addUri(magnetUri, options)
        return if (gid != null) {
            Result.success(gid)
        } else {
            Result.failure(IllegalStateException("Failed to add magnet download"))
        }
    }

    override suspend fun addTorrentFile(torrentBytes: ByteArray, saveDir: String?): Result<String> {
        val base64 = Base64.encodeToString(torrentBytes, Base64.NO_WRAP)
        val options = mapOf(
            "dir" to (saveDir ?: defaultDir()),
            "seed-time" to "0"
        )
        val gid = rpcClient.addTorrent(base64, emptyList(), options)
        return if (gid != null) {
            Result.success(gid)
        } else {
            Result.failure(IllegalStateException("Failed to add torrent download"))
        }
    }

    override suspend fun addMetalinkFile(metalinkBytes: ByteArray): Result<List<String>> {
        val base64 = Base64.encodeToString(metalinkBytes, Base64.NO_WRAP)
        val options = mapOf("dir" to defaultDir())
        val gids = rpcClient.addMetalink(base64, options)
        return if (!gids.isNullOrEmpty()) {
            Result.success(gids)
        } else {
            Result.failure(IllegalStateException("Failed to add metalink download"))
        }
    }

    override suspend fun addHttpDownload(url: String, saveDir: String?): Result<String> {
        val options = mapOf("dir" to (saveDir ?: defaultDir()))
        val gid = rpcClient.addUri(url, options)
        return if (gid != null) {
            Result.success(gid)
        } else {
            Result.failure(IllegalStateException("Failed to add HTTP download"))
        }
    }

    override suspend fun pauseDownload(gid: String): Result<Unit> {
        val res = rpcClient.pause(gid)
        return if (res != null) Result.success(Unit) else Result.failure(IllegalStateException("Failed to pause $gid"))
    }

    override suspend fun resumeDownload(gid: String): Result<Unit> {
        val res = rpcClient.unpause(gid)
        return if (res != null) Result.success(Unit) else Result.failure(IllegalStateException("Failed to resume $gid"))
    }

    override suspend fun removeDownload(gid: String): Result<Unit> {
        rpcClient.forceRemove(gid)
        rpcClient.removeDownloadResult(gid)
        return Result.success(Unit)
    }

    override suspend fun purgeCompleted(): Result<Unit> {
        rpcClient.purgeDownloadResult()
        return Result.success(Unit)
    }

    override fun observeActiveDownloads(): Flow<List<Aria2Download>> = flow {
        while (currentCoroutineContext().isActive) {
            if (processManager.isRunning.value) {
                val active = rpcClient.tellActive() ?: emptyList()
                val waiting = rpcClient.tellWaiting(0, 50) ?: emptyList()
                val stopped = rpcClient.tellStopped(0, 50) ?: emptyList()
                val allStatuses = active + waiting + stopped
                emit(allStatuses.map { it.toDomain() })
            } else {
                emit(emptyList())
            }
            delay(1000)
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun setGlobalSpeedLimit(bytesPerSecond: Long?): Result<Unit> {
        val limitStr = if (bytesPerSecond == null || bytesPerSecond <= 0) "0" else bytesPerSecond.toString()
        rpcClient.changeGlobalOption(mapOf("max-overall-download-limit" to limitStr))

        persistSetting(KEY_SPEED_LIMIT, bytesPerSecond)
        _globalSpeedLimit.value = bytesPerSecond
        return Result.success(Unit)
    }

    override fun getGlobalSpeedLimit(): StateFlow<Long?> = _globalSpeedLimit.asStateFlow()

    override suspend fun setProxy(config: ProxyConfig): Result<Unit> {
        if (config.enabled && config.host.isNotBlank()) {
            val options = mutableMapOf<String, String>(
                "all-proxy" to "http://${config.host}:${config.port}"
            )
            if (!config.username.isNullOrBlank()) {
                options["all-proxy-user"] = config.username
            }
            if (!config.password.isNullOrBlank()) {
                options["all-proxy-passwd"] = config.password
            }
            rpcClient.changeGlobalOption(options)
        } else {
            rpcClient.changeGlobalOption(mapOf("all-proxy" to ""))
        }

        persistProxy(config)
        _proxyConfig.value = config
        return Result.success(Unit)
    }

    override suspend fun setCustomHeaders(headers: Map<String, String>): Result<Unit> {
        val headerList = headers.map { "${it.key}: ${it.value}" }
        rpcClient.changeGlobalOption(mapOf("header" to headerList))

        persistHeaders(headers)
        return Result.success(Unit)
    }

    override suspend fun setCookieFile(cookieContent: String): Result<Unit> {
        val cookieFile = File(context.filesDir, "cookies.txt")
        val tmpFile = File(context.filesDir, "cookies.txt.tmp")
        tmpFile.writeText(cookieContent)
        tmpFile.renameTo(cookieFile)

        rpcClient.changeGlobalOption(mapOf("load-cookies" to cookieFile.absolutePath))
        return Result.success(Unit)
    }

    override fun getProxyConfig(): StateFlow<ProxyConfig> = _proxyConfig.asStateFlow()

    override fun getScheduleConfig(): StateFlow<ScheduleConfig> = scheduleConfigStore.config

    override suspend fun setScheduleConfig(config: ScheduleConfig): Result<Unit> {
        return scheduleConfigStore.saveConfig(config)
    }

    override suspend fun pauseAllActive(): Result<Unit> {
        rpcClient.pauseAll()
        return Result.success(Unit)
    }

    override suspend fun resumeAllPaused(): Result<Unit> {
        rpcClient.unpauseAll()
        return Result.success(Unit)
    }

    override suspend fun getVersionInfo(): Pair<String, List<String>>? {
        val version = rpcClient.getVersion() ?: return null
        return Pair(version.version, version.enabledFeatures)
    }

    override suspend fun setMaxPeers(peers: Int): Result<Unit> {
        rpcClient.changeGlobalOption(mapOf("bt-max-peers" to peers.toString()))
        persistSetting("bt_max_peers", peers)
        return Result.success(Unit)
    }

    override suspend fun setSeedTime(minutes: Int): Result<Unit> {
        rpcClient.changeGlobalOption(mapOf("seed-time" to minutes.toString()))
        persistSetting("seed_time", minutes)
        return Result.success(Unit)
    }

    private fun Aria2Status.toDomain(): Aria2Download {
        val domainStatus = when (status) {
            "active" -> Aria2DownloadStatus.ACTIVE
            "waiting" -> Aria2DownloadStatus.WAITING
            "paused" -> Aria2DownloadStatus.PAUSED
            "complete" -> Aria2DownloadStatus.COMPLETE
            "error" -> Aria2DownloadStatus.ERROR
            "removed" -> Aria2DownloadStatus.REMOVED
            else -> Aria2DownloadStatus.ACTIVE
        }

        val name = bittorrent?.info?.name
            ?: files?.firstOrNull()?.path?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: files?.firstOrNull()?.uris?.firstOrNull()?.uri?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "Download $gid"

        val seedersCount = numSeeders?.toIntOrNull() ?: if (seeder == "true") 1 else 0

        return Aria2Download(
            gid = gid,
            name = name,
            status = domainStatus,
            totalBytes = totalLength?.toLongOrNull() ?: 0L,
            completedBytes = completedLength?.toLongOrNull() ?: 0L,
            downloadSpeed = downloadSpeed?.toLongOrNull() ?: 0L,
            uploadSpeed = uploadSpeed?.toLongOrNull() ?: 0L,
            connections = connections?.toIntOrNull() ?: 0,
            seeders = seedersCount,
            isTorrent = bittorrent != null,
            isMetalink = false,
            errorMessage = errorMessage
        )
    }

    private fun persistProxy(config: ProxyConfig) {
        try {
            val root = loadSettingsJson()
            val obj = JSONObject().apply {
                put("enabled", config.enabled)
                put("host", config.host)
                put("port", config.port)
                put("username", config.username ?: "")
                put("password", config.password ?: "")
            }
            root.put(KEY_PROXY, obj)
            writeSettingsJson(root)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist proxy", e)
        }
    }

    private fun loadProxyConfig(): ProxyConfig {
        return try {
            val root = loadSettingsJson()
            if (!root.has(KEY_PROXY)) return ProxyConfig()
            val obj = root.getJSONObject(KEY_PROXY)
            ProxyConfig(
                enabled = obj.optBoolean("enabled", false),
                host = obj.optString("host", ""),
                port = obj.optInt("port", 8080),
                username = obj.optString("username").takeIf { it.isNotBlank() },
                password = obj.optString("password").takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load proxy config", e)
            ProxyConfig()
        }
    }

    private fun persistHeaders(headers: Map<String, String>) {
        try {
            val root = loadSettingsJson()
            val obj = JSONObject()
            headers.forEach { (k, v) -> obj.put(k, v) }
            root.put(KEY_HEADERS, obj)
            writeSettingsJson(root)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist headers", e)
        }
    }

    private fun persistSetting(key: String, value: Any?) {
        try {
            val root = loadSettingsJson()
            if (value == null) {
                root.remove(key)
            } else {
                root.put(key, value)
            }
            writeSettingsJson(root)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist setting $key", e)
        }
    }

    private fun loadGlobalSpeedLimit(): Long? {
        return try {
            val root = loadSettingsJson()
            if (root.has(KEY_SPEED_LIMIT)) root.getLong(KEY_SPEED_LIMIT) else null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load global speed limit", e)
            null
        }
    }

    private fun loadSettingsJson(): JSONObject {
        return if (settingsFile.exists()) {
            try { JSONObject(settingsFile.readText()) } catch (e: Exception) { Log.w(TAG, "Failed to read settings JSON", e); JSONObject() }
        } else {
            JSONObject()
        }
    }

    private fun writeSettingsJson(root: JSONObject) {
        val tmp = File(context.filesDir, "settings.json.tmp")
        tmp.writeText(root.toString(2))
        tmp.renameTo(settingsFile)
    }


    override fun close() {
        scope.cancel()
        try {
            processManager.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop aria2 process", e)
        }
    }
}
