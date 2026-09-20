package com.bettertube.app.data.repository

import android.content.Context
import android.util.Log
import com.bettertube.app.data.engine.YtDlpEngine
import com.bettertube.app.domain.model.DownloadStatus
import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.domain.model.ExtractionPreset
import com.bettertube.app.domain.model.MediaMetadata
import com.bettertube.app.domain.model.MediaType
import com.bettertube.app.domain.model.PlaylistInfo
import com.bettertube.app.domain.model.SubtitleTrack
import com.bettertube.app.domain.repository.DownloadRepository
import com.bettertube.app.service.DownloadForegroundService
import com.bettertube.app.utils.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl(
    private val engine: YtDlpEngine,
    private val networkMonitor: NetworkMonitor,
    private val context: Context?
) : DownloadRepository {

    private val _tasks = MutableStateFlow<Map<String, DownloadTask>>(emptyMap())
    private val _globalSpeedLimit = MutableStateFlow<Long?>(null)
    private val _wifiOnly = MutableStateFlow<Boolean>(false)
    private val _queueOrder = MutableStateFlow<List<String>>(emptyList())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private var repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    constructor(
        engine: YtDlpEngine,
        context: Context?,
        testScope: CoroutineScope,
        networkMonitor: NetworkMonitor = NetworkMonitor(context)
    ) : this(engine, networkMonitor, context) {
        this.repositoryScope = testScope
    }

    constructor(
        engine: YtDlpEngine,
        context: Context?,
        testScope: CoroutineScope
    ) : this(engine, NetworkMonitor(context), context) {
        this.repositoryScope = testScope
    }

    init {
        loadPersistedSettings()
        loadPersistedQueue()
        loadPersistedTasks()
    }

    override suspend fun fetchMetadata(url: String): Result<MediaMetadata> {
        return engine.extractInfo(url)
    }

    override suspend fun fetchPlaylist(url: String): Result<PlaylistInfo> {
        return engine.extractPlaylist(url)
    }

    override suspend fun fetchSubtitles(url: String): Result<List<SubtitleTrack>> {
        return engine.extractSubtitles(url)
    }

    override suspend fun startPlaylistDownload(
        url: String,
        selectedIndices: List<Int>,
        formatId: String,
        preset: ExtractionPreset,
        downloadSubtitles: Boolean,
        subtitleLanguages: List<String>,
        embedSubtitles: Boolean
    ): Result<List<String>> {
        val playlistResult = engine.extractPlaylist(url)
        if (playlistResult.isFailure) {
            return Result.failure(playlistResult.exceptionOrNull() ?: Exception("Failed to fetch playlist"))
        }

        val playlist = playlistResult.getOrThrow()
        val selectedEntries = playlist.entries.filter { it.index in selectedIndices }
        if (selectedEntries.isEmpty()) {
            return Result.failure(IllegalArgumentException("No entries selected for download"))
        }

        val mediaType = if (preset.name.startsWith("AUDIO_")) MediaType.AUDIO else MediaType.VIDEO
        val baseTime = System.currentTimeMillis()
        val createdTasks = mutableListOf<DownloadTask>()

        selectedEntries.forEach { entry ->
            val taskId = UUID.randomUUID().toString()
            val task = DownloadTask(
                id = taskId,
                url = entry.url,
                title = entry.title,
                thumbnailUrl = entry.thumbnailUrl,
                formatId = formatId,
                status = DownloadStatus.WAITING,
                progressPercent = 0.0f,
                downloadedBytes = 0L,
                totalBytes = 0L,
                speedBytesPerSecond = 0L,
                etaSeconds = 0L,
                outputFilePath = null,
                errorMessage = null,
                createdAtMillis = baseTime + entry.index,
                mediaType = mediaType,
                preset = preset,
                downloadSubtitles = downloadSubtitles,
                subtitleLanguages = subtitleLanguages,
                embedSubtitles = embedSubtitles,
                isPlaylistChild = true,
                playlistParentUrl = url
            )
            createdTasks.add(task)
        }

        _tasks.update { it + createdTasks.associateBy { t -> t.id } }
        _queueOrder.update { it + createdTasks.map { t -> t.id } }
        persistTasks()
        persistQueue()

        repositoryScope.launch {
            for (task in createdTasks) {
                val currentTask = _tasks.value[task.id] ?: continue
                if (currentTask.status == DownloadStatus.CANCELLED) continue
                executeDownloadTask(currentTask)
            }
        }

        return Result.success(createdTasks.map { it.id })
    }

    private suspend fun executeDownloadTask(task: DownloadTask): Result<String> {
        if (_wifiOnly.value && !networkMonitor.isOnWifi()) {
            val waitingTask = task.copy(
                status = DownloadStatus.WAITING,
                errorMessage = "Waiting for WiFi",
                lastAttemptUrl = task.url
            )
            _tasks.update { it + (task.id to waitingTask) }
            _queueOrder.update { if (!it.contains(task.id)) it + task.id else it }
            persistTasks()
            persistQueue()
            return Result.success(task.id)
        }

        val processId = UUID.randomUUID().toString()
        val initialTask = task.copy(
            status = DownloadStatus.EXTRACTING,
            processId = processId,
            lastAttemptUrl = task.url
        )
        _tasks.update { it + (task.id to initialTask) }
        persistTasks()

        try {
            runCatching { context?.let { DownloadForegroundService.start(it) } }

            _tasks.update { currentMap ->
                val current = currentMap[task.id] ?: initialTask
                currentMap + (task.id to current.copy(status = DownloadStatus.DOWNLOADING))
            }
            persistTasks()

            val effectiveLimit = task.speedLimitBytesPerSecond ?: _globalSpeedLimit.value
            val downloadResult = engine.startDownload(
                url = task.url,
                formatId = task.formatId,
                taskId = task.id,
                processId = processId,
                speedLimitBytesPerSecond = effectiveLimit,
                preset = task.preset,
                downloadSubtitles = task.downloadSubtitles,
                subtitleLanguages = task.subtitleLanguages,
                embedSubtitles = task.embedSubtitles,
                allowPlaylist = false
            ) { percent, downloadedBytes, totalBytes, speed, etaSeconds ->
                _tasks.update { currentMap ->
                    val current = currentMap[task.id] ?: task
                    val updated = current.copy(
                        status = DownloadStatus.DOWNLOADING,
                        progressPercent = percent,
                        downloadedBytes = downloadedBytes,
                        totalBytes = if (totalBytes > 0) totalBytes else current.totalBytes,
                        speedBytesPerSecond = speed,
                        etaSeconds = etaSeconds
                    )
                    currentMap + (task.id to updated)
                }
            }

            return downloadResult.fold(
                onSuccess = {
                    val outputDir = runCatching { context?.getExternalFilesDir(null as String?) }.getOrNull()
                    val ext = if (task.mediaType == MediaType.AUDIO) "mp3" else "mp4"
                    val outputFilePath = if (outputDir != null) {
                        "${outputDir.absolutePath}/BetterTube/${task.title}.$ext"
                    } else null

                    _tasks.update { currentMap ->
                        val current = currentMap[task.id] ?: task
                        val updated = current.copy(
                            status = DownloadStatus.COMPLETE,
                            progressPercent = 1.0f,
                            outputFilePath = outputFilePath,
                            errorMessage = null
                        )
                        currentMap + (task.id to updated)
                    }
                    persistTasks()
                    Result.success(task.id)
                },
                onFailure = { error ->
                    _tasks.update { currentMap ->
                        val current = currentMap[task.id] ?: task
                        val updated = current.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = error.message ?: "Interrupted or failed"
                        )
                        currentMap + (task.id to updated)
                    }
                    persistTasks()
                    Result.failure(error)
                }
            )
        } finally {
            activeJobs.remove(task.id)
        }
    }

    override suspend fun startDownload(task: DownloadTask): Result<String> {
        val job = repositoryScope.launch {
            executeDownloadTask(task)
        }
        activeJobs[task.id] = job
        return Result.success(task.id)
    }

    override suspend fun pauseDownload(id: String): Result<Unit> {
        val task = _tasks.value[id] ?: return Result.failure(IllegalStateException("Task not found"))
        if (task.status != DownloadStatus.DOWNLOADING) {
            return Result.failure(IllegalStateException("Task is not downloading"))
        }

        activeJobs.remove(id)?.cancel()
        val pauseResult = engine.pauseProcess(task.processId ?: "")
        if (pauseResult.isFailure) {
            Log.w("DownloadRepository", "Process already exited or pause failed for task $id")
        }

        _tasks.update { currentMap ->
            val current = currentMap[id] ?: task
            currentMap + (id to current.copy(
                status = DownloadStatus.PAUSED,
                speedBytesPerSecond = 0L,
                etaSeconds = 0L
            ))
        }
        persistTasks()
        return Result.success(Unit)
    }

    override suspend fun resumeDownload(id: String): Result<Unit> {
        val task = _tasks.value[id] ?: return Result.failure(IllegalStateException("Task not found"))
        if (task.status != DownloadStatus.PAUSED && task.status != DownloadStatus.FAILED) {
            return Result.failure(IllegalStateException("Task cannot be resumed"))
        }

        val newProcessId = UUID.randomUUID().toString()
        val newRetryCount = if (task.status == DownloadStatus.FAILED) task.retryCount + 1 else task.retryCount
        val targetUrl = task.lastAttemptUrl ?: task.url

        val updatedTask = task.copy(
            status = DownloadStatus.EXTRACTING,
            processId = newProcessId,
            retryCount = newRetryCount,
            lastAttemptUrl = targetUrl
        )
        _tasks.update { it + (id to updatedTask) }
        persistTasks()

        val job = repositoryScope.launch {
            try {
                runCatching { context?.let { DownloadForegroundService.start(it) } }

                _tasks.update { currentMap ->
                    val current = currentMap[id] ?: updatedTask
                    currentMap + (id to current.copy(status = DownloadStatus.DOWNLOADING))
                }
                persistTasks()

                val effectiveLimit = task.speedLimitBytesPerSecond ?: _globalSpeedLimit.value
                val downloadResult = engine.startDownload(
                    url = targetUrl,
                    formatId = task.formatId,
                    taskId = task.id,
                    processId = newProcessId,
                    speedLimitBytesPerSecond = effectiveLimit,
                    preset = task.preset,
                    downloadSubtitles = task.downloadSubtitles,
                    subtitleLanguages = task.subtitleLanguages,
                    embedSubtitles = task.embedSubtitles,
                    allowPlaylist = false
                ) { percent, downloadedBytes, totalBytes, speed, etaSeconds ->
                    _tasks.update { currentMap ->
                        val current = currentMap[id] ?: updatedTask
                        val updated = current.copy(
                            status = DownloadStatus.DOWNLOADING,
                            progressPercent = percent,
                            downloadedBytes = downloadedBytes,
                            totalBytes = if (totalBytes > 0) totalBytes else current.totalBytes,
                            speedBytesPerSecond = speed,
                            etaSeconds = etaSeconds
                        )
                        currentMap + (id to updated)
                    }
                }

                downloadResult.fold(
                    onSuccess = {
                        val outputDir = runCatching { context?.getExternalFilesDir(null as String?) }.getOrNull()
                        val ext = if (task.mediaType == MediaType.AUDIO) "mp3" else "mp4"
                        val outputFilePath = if (outputDir != null) {
                            "${outputDir.absolutePath}/BetterTube/${task.title}.$ext"
                        } else null

                        _tasks.update { currentMap ->
                            val current = currentMap[id] ?: updatedTask
                            val updated = current.copy(
                                status = DownloadStatus.COMPLETE,
                                progressPercent = 1.0f,
                                outputFilePath = outputFilePath,
                                errorMessage = null
                            )
                            currentMap + (id to updated)
                        }
                        persistTasks()
                    },
                    onFailure = { error ->
                        _tasks.update { currentMap ->
                            val current = currentMap[id] ?: updatedTask
                            val updated = current.copy(
                                status = DownloadStatus.FAILED,
                                errorMessage = error.message ?: "Interrupted or failed"
                            )
                            currentMap + (id to updated)
                        }
                        persistTasks()
                    }
                )
            } finally {
                activeJobs.remove(id)
            }
        }
        activeJobs[id] = job

        return Result.success(Unit)
    }

    override suspend fun retryDownload(id: String): Result<Unit> {
        val task = _tasks.value[id] ?: return Result.failure(IllegalStateException("Task not found"))
        if (task.status != DownloadStatus.PAUSED && task.status != DownloadStatus.FAILED) {
            return Result.failure(IllegalStateException("Task cannot be resumed"))
        }

        // Reset retryCount to 0 and clear errorMessage
        val resetTask = task.copy(
            retryCount = 0,
            errorMessage = null
        )
        _tasks.update { it + (id to resetTask) }
        persistTasks()

        return resumeDownload(id)
    }

    override suspend fun cancelDownload(id: String): Result<Unit> {
        activeJobs.remove(id)?.cancel()
        val task = _tasks.value[id]
        if (task != null) {
            if (task.status == DownloadStatus.DOWNLOADING) {
                engine.pauseProcess(task.processId ?: "")
            }

            // Edge Case 2: If status == COMPLETE, do not delete file on disk
            if (task.status != DownloadStatus.COMPLETE) {
                deletePartialFiles(task)
            }
        }

        _tasks.update { it - id }
        _queueOrder.update { it - id }
        persistTasks()
        persistQueue()

        return Result.success(Unit)
    }

    private fun deletePartialFiles(task: DownloadTask) {
        try {
            task.outputFilePath?.let { path ->
                val file = File(path)
                if (file.exists()) file.delete()
                val aria2File = File("$path.aria2")
                if (aria2File.exists()) aria2File.delete()
            }

            val parentDir = context?.getExternalFilesDir(null) ?: File(System.getProperty("java.io.tmpdir", "/tmp"))
            val downloadDir = File(parentDir, "BetterTube")
            if (downloadDir.exists()) {
                downloadDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith(task.title)) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {
            // Best-effort cleanup
        }
    }

    override suspend fun setDownloadSpeedLimit(id: String, bytesPerSecond: Long?): Result<Unit> {
        val task = _tasks.value[id] ?: return Result.failure(IllegalStateException("Task not found"))
        val cleanLimit = if (bytesPerSecond != null && bytesPerSecond <= 0) null else bytesPerSecond
        val updated = task.copy(speedLimitBytesPerSecond = cleanLimit)
        _tasks.update { it + (id to updated) }
        persistTasks()

        if (task.status == DownloadStatus.DOWNLOADING) {
            pauseDownload(id)
            resumeDownload(id)
        }
        return Result.success(Unit)
    }

    override suspend fun setGlobalSpeedLimit(bytesPerSecond: Long?): Result<Unit> {
        val cleanLimit = if (bytesPerSecond != null && bytesPerSecond <= 0) null else bytesPerSecond
        _globalSpeedLimit.value = cleanLimit
        persistSettings()

        _tasks.value.values.forEach { task ->
            if (task.status == DownloadStatus.DOWNLOADING && task.speedLimitBytesPerSecond == null) {
                pauseDownload(task.id)
                resumeDownload(task.id)
            }
        }
        return Result.success(Unit)
    }

    override suspend fun reorderQueue(orderedIds: List<String>): Result<Unit> {
        // Edge Case 7: validate duplicate IDs and all IDs exist and are WAITING
        if (orderedIds.toSet().size != orderedIds.size) {
            return Result.failure(IllegalArgumentException("Duplicate IDs in queue"))
        }

        for (id in orderedIds) {
            val task = _tasks.value[id]
            if (task == null || task.status != DownloadStatus.WAITING) {
                return Result.failure(IllegalArgumentException("Task $id is not in WAITING state"))
            }
        }

        _queueOrder.value = orderedIds
        persistQueue()
        return Result.success(Unit)
    }

    override fun getGlobalSpeedLimit(): StateFlow<Long?> = _globalSpeedLimit.asStateFlow()

    override fun getWifiOnly(): StateFlow<Boolean> = _wifiOnly.asStateFlow()

    override suspend fun setWifiOnly(enabled: Boolean): Result<Unit> {
        _wifiOnly.value = enabled
        persistSettings()

        if (enabled && !networkMonitor.isOnWifi()) {
            _tasks.value.values
                .filter { it.status == DownloadStatus.DOWNLOADING }
                .forEach { pauseDownload(it.id) }
        }
        return Result.success(Unit)
    }

    override fun getAllTasks(): Flow<List<DownloadTask>> {
        return _tasks.map { it.values.sortedByDescending { task -> task.createdAtMillis } }
    }

    override fun getTask(id: String): Flow<DownloadTask?> {
        return _tasks.map { it[id] }
    }

    private fun getStorageFile(name: String): File? {
        val dir = runCatching { context?.filesDir }.getOrNull() ?: return null
        return File(dir, name)
    }

    private fun atomicWrite(file: File, content: String) {
        val dir = file.parentFile ?: return
        if (!dir.exists()) dir.mkdirs()
        val tempFile = File(dir, "${file.name}.${System.currentTimeMillis()}.tmp")
        tempFile.writeText(content)
        if (!tempFile.renameTo(file)) {
            file.writeText(content)
            tempFile.delete()
        }
    }

    private fun loadPersistedSettings() {
        try {
            val file = getStorageFile("settings.json") ?: return
            if (!file.exists()) return
            val jsonString = file.readText()
            if (jsonString.isBlank()) return
            val obj = JSONObject(jsonString)
            _globalSpeedLimit.value = if (obj.has("globalSpeedLimit")) obj.getLong("globalSpeedLimit") else null
            _wifiOnly.value = obj.optBoolean("wifiOnly", false)
        } catch (_: Exception) {
            // Recoverable
        }
    }

    private fun persistSettings() {
        try {
            val file = getStorageFile("settings.json") ?: return
            val obj = JSONObject().apply {
                _globalSpeedLimit.value?.let { put("globalSpeedLimit", it) }
                put("wifiOnly", _wifiOnly.value)
            }
            atomicWrite(file, obj.toString())
        } catch (_: Exception) {
            // Recoverable
        }
    }

    private fun loadPersistedQueue() {
        try {
            val file = getStorageFile("queue.json") ?: return
            if (!file.exists()) return
            val jsonString = file.readText()
            if (jsonString.isBlank()) return
            val array = JSONArray(jsonString)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                list.add(array.getString(i))
            }
            _queueOrder.value = list
        } catch (_: Exception) {
            // Recoverable
        }
    }

    private fun persistQueue() {
        try {
            val file = getStorageFile("queue.json") ?: return
            val array = JSONArray()
            _queueOrder.value.forEach { array.put(it) }
            atomicWrite(file, array.toString())
        } catch (_: Exception) {
            // Recoverable
        }
    }

    private fun loadPersistedTasks() {
        try {
            val file = getStorageFile("tasks.json") ?: return
            if (!file.exists()) return
            val jsonString = file.readText()
            if (jsonString.isBlank()) return

            val array = JSONArray(jsonString)
            val loadedMap = mutableMapOf<String, DownloadTask>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val statusString = obj.optString("status", DownloadStatus.FAILED.name)
                var status = runCatching { DownloadStatus.valueOf(statusString) }.getOrDefault(DownloadStatus.FAILED)
                var errorMessage = if (obj.has("errorMessage")) obj.getString("errorMessage") else null

                // Edge Case 4 & backward compat: mark DOWNLOADING, EXTRACTING, MERGING as FAILED "Interrupted"
                if (status == DownloadStatus.DOWNLOADING || status == DownloadStatus.EXTRACTING ||
                    status == DownloadStatus.MERGING || status == DownloadStatus.QUEUED
                ) {
                    status = DownloadStatus.FAILED
                    errorMessage = "Interrupted"
                }

                val mediaTypeString = obj.optString("mediaType", MediaType.OTHER.name)
                val mediaType = runCatching { MediaType.valueOf(mediaTypeString) }.getOrDefault(MediaType.OTHER)
                val presetString = obj.optString("preset", ExtractionPreset.VIDEO_ORIGINAL.name)
                val preset = runCatching { ExtractionPreset.valueOf(presetString) }.getOrDefault(ExtractionPreset.VIDEO_ORIGINAL)
                val downloadSubtitles = obj.optBoolean("downloadSubtitles", false)
                val embedSubtitles = obj.optBoolean("embedSubtitles", false)
                val isPlaylistChild = obj.optBoolean("isPlaylistChild", false)
                val playlistParentUrl = if (obj.has("playlistParentUrl")) obj.getString("playlistParentUrl") else null
                val subtitleLanguages = mutableListOf<String>()
                if (obj.has("subtitleLanguages")) {
                    val subArr = obj.getJSONArray("subtitleLanguages")
                    for (sIdx in 0 until subArr.length()) {
                        subtitleLanguages.add(subArr.getString(sIdx))
                    }
                }
                val processId = if (obj.has("processId")) obj.getString("processId") else null
                val lastAttemptUrl = if (obj.has("lastAttemptUrl")) obj.getString("lastAttemptUrl") else null
                val retryCount = obj.optInt("retryCount", 0)
                val speedLimitBytesPerSecond = if (obj.has("speedLimitBytesPerSecond")) obj.getLong("speedLimitBytesPerSecond") else null

                val task = DownloadTask(
                    id = obj.getString("id"),
                    url = obj.getString("url"),
                    title = obj.getString("title"),
                    thumbnailUrl = obj.optString("thumbnailUrl", ""),
                    formatId = obj.optString("formatId", "best"),
                    status = status,
                    progressPercent = obj.optDouble("progressPercent", 0.0).toFloat(),
                    downloadedBytes = obj.optLong("downloadedBytes", 0L),
                    totalBytes = obj.optLong("totalBytes", 0L),
                    speedBytesPerSecond = obj.optLong("speedBytesPerSecond", 0L),
                    etaSeconds = obj.optLong("etaSeconds", 0L),
                    outputFilePath = if (obj.has("outputFilePath")) obj.getString("outputFilePath") else null,
                    errorMessage = errorMessage,
                    createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                    mediaType = mediaType,
                    processId = processId,
                    lastAttemptUrl = lastAttemptUrl,
                    retryCount = retryCount,
                    speedLimitBytesPerSecond = speedLimitBytesPerSecond,
                    preset = preset,
                    downloadSubtitles = downloadSubtitles,
                    subtitleLanguages = subtitleLanguages,
                    embedSubtitles = embedSubtitles,
                    isPlaylistChild = isPlaylistChild,
                    playlistParentUrl = playlistParentUrl
                )
                loadedMap[task.id] = task
            }
            _tasks.value = loadedMap
        } catch (_: Exception) {
            // Silently recover if cache file was corrupted
        }
    }

    private fun persistTasks() {
        try {
            val file = getStorageFile("tasks.json") ?: return
            val array = JSONArray()
            _tasks.value.values.forEach { task ->
                val obj = JSONObject().apply {
                    put("id", task.id)
                    put("url", task.url)
                    put("title", task.title)
                    put("thumbnailUrl", task.thumbnailUrl)
                    put("formatId", task.formatId)
                    put("status", task.status.name)
                    put("progressPercent", task.progressPercent.toDouble())
                    put("downloadedBytes", task.downloadedBytes)
                    put("totalBytes", task.totalBytes)
                    put("speedBytesPerSecond", task.speedBytesPerSecond)
                    put("etaSeconds", task.etaSeconds)
                    task.outputFilePath?.let { put("outputFilePath", it) }
                    task.errorMessage?.let { put("errorMessage", it) }
                    put("createdAtMillis", task.createdAtMillis)
                    put("mediaType", task.mediaType.name)
                    task.processId?.let { put("processId", it) }
                    task.lastAttemptUrl?.let { put("lastAttemptUrl", it) }
                    put("retryCount", task.retryCount)
                    task.speedLimitBytesPerSecond?.let { put("speedLimitBytesPerSecond", it) }
                    put("preset", task.preset.name)
                    put("downloadSubtitles", task.downloadSubtitles)
                    put("embedSubtitles", task.embedSubtitles)
                    put("isPlaylistChild", task.isPlaylistChild)
                    task.playlistParentUrl?.let { put("playlistParentUrl", it) }
                    val subArr = JSONArray()
                    task.subtitleLanguages.forEach { subArr.put(it) }
                    put("subtitleLanguages", subArr)
                }
                array.put(obj)
            }
            atomicWrite(file, array.toString())
        } catch (_: Exception) {
            // Non-fatal persistence
        }
    }
}
