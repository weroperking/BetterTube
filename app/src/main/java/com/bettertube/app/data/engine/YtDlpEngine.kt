package com.bettertube.app.data.engine

import android.content.Context
import com.bettertube.app.domain.model.ExtractionPreset
import com.bettertube.app.domain.model.MediaFormat
import com.bettertube.app.domain.model.MediaMetadata
import com.bettertube.app.domain.model.PlaylistEntry
import com.bettertube.app.domain.model.PlaylistInfo
import com.bettertube.app.domain.model.SubtitleTrack
import com.bettertube.app.domain.model.toYtDlpArgs
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject

open class YtDlpEngine @Inject constructor(
    @ApplicationContext private val context: Context? = null
) {
    open suspend fun extractInfo(url: String): Result<MediaMetadata> = withContext(Dispatchers.IO) {
        try {
            val videoInfo = YoutubeDL.getInstance().getInfo(url)
            val mappedFormats = (videoInfo.formats ?: emptyList())
                .filter { format ->
                    val note = format.formatNote ?: ""
                    val vcodec = format.vcodec ?: "none"
                    val acodec = format.acodec ?: "none"
                    !note.contains("storyboard", ignoreCase = true) &&
                            !(vcodec == "none" && acodec == "none")
                }
                .map { format ->
                    val vcodec = format.vcodec ?: "none"
                    val acodec = format.acodec ?: "none"
                    val isVideo = vcodec != "none"
                    val isAudio = acodec != "none"
                    val fileSizeBytes = when {
                        format.fileSize > 0 -> format.fileSize
                        format.fileSizeApproximate > 0 -> format.fileSizeApproximate
                        else -> null
                    }
                    val resolution = if (format.height > 0) "${format.height}p" else null
                    val videoBitrate = if (format.tbr > 0) format.tbr.toInt() else null
                    MediaFormat(
                        formatId = format.formatId ?: "",
                        extension = format.ext ?: "",
                        resolution = resolution,
                        audioBitrate = if (format.abr > 0) format.abr else null,
                        videoBitrate = videoBitrate,
                        fileSizeBytes = fileSizeBytes,
                        note = format.formatNote ?: "",
                        isVideo = isVideo,
                        isAudio = isAudio
                    )
                }
                .sortedWith(
                    compareByDescending<MediaFormat> { format ->
                        format.resolution?.filter { it.isDigit() }?.toIntOrNull() ?: 0
                    }.thenByDescending { format ->
                        format.videoBitrate ?: format.audioBitrate ?: 0
                    }
                )

            val metadata = MediaMetadata(
                id = videoInfo.id ?: "",
                title = videoInfo.title ?: "",
                uploader = videoInfo.uploader ?: "",
                durationSeconds = videoInfo.duration.toLong(),
                thumbnailUrl = videoInfo.thumbnail ?: "",
                webpageUrl = videoInfo.webpageUrl ?: url,
                formats = mappedFormats,
                extractor = videoInfo.extractor ?: ""
            )
            Result.success(metadata)
        } catch (e: Exception) {
            Result.failure(EngineException.ExtractionFailed(e.message ?: "Extraction failed", e))
        }
    }

    open suspend fun extractPlaylist(url: String): Result<PlaylistInfo> = withContext(Dispatchers.IO) {
        try {
            val request = YoutubeDLRequest(url).apply {
                addOption("--dump-single-json")
                addOption("--flat-playlist")
                addOption("--no-warnings")
            }
            val response = YoutubeDL.getInstance().execute(request)
            val jsonString = response.out
            val jsonObj = JSONObject(jsonString)

            val type = jsonObj.optString("_type", "")
            val entriesArray = jsonObj.optJSONArray("entries")
            if (type != "playlist" && entriesArray == null) {
                return@withContext Result.failure(EngineException.UnsupportedUrl("URL is not a playlist"))
            }

            val entries = mutableListOf<PlaylistEntry>()
            if (entriesArray != null) {
                for (i in 0 until entriesArray.length()) {
                    val entryObj = entriesArray.optJSONObject(i) ?: continue
                    val entryUrl = entryObj.optString("webpage_url", entryObj.optString("url", ""))
                    val title = entryObj.optString("title", "Track ${i + 1}")
                    val id = entryObj.optString("id", UUID.randomUUID().toString())
                    val duration = entryObj.optLong("duration", 0L)
                    val thumb = entryObj.optString("thumbnail", "")

                    val finalUrl = if (entryUrl.startsWith("http")) entryUrl else if (id.isNotBlank()) "https://www.youtube.com/watch?v=$id" else url

                    entries.add(
                        PlaylistEntry(
                            index = i,
                            id = id,
                            title = title,
                            durationSeconds = duration,
                            thumbnailUrl = thumb,
                            url = finalUrl
                        )
                    )
                }
            }

            val playlistInfo = PlaylistInfo(
                id = jsonObj.optString("id", UUID.randomUUID().toString()),
                title = jsonObj.optString("title", "Playlist"),
                uploader = jsonObj.optString("uploader", ""),
                entryCount = entries.size,
                entries = entries,
                extractor = jsonObj.optString("extractor", "")
            )
            Result.success(playlistInfo)
        } catch (e: Exception) {
            Result.failure(EngineException.ExtractionFailed(e.message ?: "Failed to extract playlist", e))
        }
    }

    open suspend fun extractSubtitles(url: String): Result<List<SubtitleTrack>> = withContext(Dispatchers.IO) {
        try {
            val request = YoutubeDLRequest(url).apply {
                addOption("--dump-single-json")
                addOption("--skip-download")
                addOption("--no-warnings")
            }
            val response = YoutubeDL.getInstance().execute(request)
            val jsonObj = JSONObject(response.out)
            val tracks = mutableListOf<SubtitleTrack>()

            val subsObj = jsonObj.optJSONObject("subtitles")
            if (subsObj != null) {
                val keys = subsObj.keys()
                while (keys.hasNext()) {
                    val lang = keys.next()
                    val formatsArray = subsObj.optJSONArray(lang)
                    val trackUrl = formatsArray?.optJSONObject(0)?.optString("url", "") ?: ""
                    tracks.add(
                        SubtitleTrack(
                            languageCode = lang,
                            languageName = lang,
                            isAutoGenerated = false,
                            url = trackUrl
                        )
                    )
                }
            }

            val autoCaptionsObj = jsonObj.optJSONObject("automatic_captions")
            if (autoCaptionsObj != null) {
                val keys = autoCaptionsObj.keys()
                while (keys.hasNext()) {
                    val lang = keys.next()
                    val formatsArray = autoCaptionsObj.optJSONArray(lang)
                    val trackUrl = formatsArray?.optJSONObject(0)?.optString("url", "") ?: ""
                    tracks.add(
                        SubtitleTrack(
                            languageCode = lang,
                            languageName = "$lang (auto)",
                            isAutoGenerated = true,
                            url = trackUrl
                        )
                    )
                }
            }

            Result.success(tracks)
        } catch (e: Exception) {
            Result.failure(EngineException.ExtractionFailed(e.message ?: "Failed to extract subtitles", e))
        }
    }

    open suspend fun startDownload(
        url: String,
        formatId: String,
        taskId: String,
        processId: String,
        speedLimitBytesPerSecond: Long?,
        onProgress: (percent: Float, downloadedBytes: Long, totalBytes: Long, speed: Long, etaSeconds: Long) -> Unit
    ): Result<String> {
        return startDownload(
            url = url,
            formatId = formatId,
            taskId = taskId,
            processId = processId,
            speedLimitBytesPerSecond = speedLimitBytesPerSecond,
            preset = ExtractionPreset.VIDEO_ORIGINAL,
            downloadSubtitles = false,
            subtitleLanguages = emptyList(),
            embedSubtitles = false,
            allowPlaylist = false,
            onProgress = onProgress
        )
    }

    open suspend fun startDownload(
        url: String,
        formatId: String,
        taskId: String,
        processId: String = taskId,
        speedLimitBytesPerSecond: Long? = null,
        preset: ExtractionPreset = ExtractionPreset.VIDEO_ORIGINAL,
        downloadSubtitles: Boolean = false,
        subtitleLanguages: List<String> = emptyList(),
        embedSubtitles: Boolean = false,
        allowPlaylist: Boolean = false,
        onProgress: (percent: Float, downloadedBytes: Long, totalBytes: Long, speed: Long, etaSeconds: Long) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val parentDir = context?.getExternalFilesDir(null) ?: File(System.getProperty("java.io.tmpdir", "/tmp"))
            val downloadDir = File(parentDir, "BetterTube")
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            val aria2Args = if (speedLimitBytesPerSecond != null && speedLimitBytesPerSecond > 0) {
                "aria2c:-x 16 -s 16 -k 1M --console-log-level=warn --max-download-limit=$speedLimitBytesPerSecond"
            } else {
                "aria2c:-x 16 -s 16 -k 1M --console-log-level=warn"
            }

            val isAudioPreset = preset.name.startsWith("AUDIO_")
            val effectiveFormatId = if (isAudioPreset && formatId.isBlank()) {
                "bestaudio/best"
            } else {
                formatId
            }

            val request = YoutubeDLRequest(url).apply {
                if (effectiveFormatId.isNotBlank()) {
                    addOption("-f", effectiveFormatId)
                }
                addOption("--downloader", "aria2c")
                addOption("--downloader-args", aria2Args)

                // Apply preset args
                val presetArgs = preset.toYtDlpArgs()
                var i = 0
                while (i < presetArgs.size) {
                    if (i + 1 < presetArgs.size && !presetArgs[i + 1].startsWith("-")) {
                        addOption(presetArgs[i], presetArgs[i + 1])
                        i += 2
                    } else {
                        addOption(presetArgs[i])
                        i += 1
                    }
                }

                if (!allowPlaylist) {
                    addOption("--no-playlist")
                }

                if (downloadSubtitles) {
                    addOption("--write-subs")
                    addOption("--write-auto-subs")
                    if (subtitleLanguages.isNotEmpty()) {
                        addOption("--sub-langs", subtitleLanguages.joinToString(","))
                    }
                }
                if (embedSubtitles) {
                    addOption("--embed-subs")
                }

                addOption("--continue")
                addOption("--no-overwrites")
                addOption("--no-part")
                addOption("-o", "${downloadDir.absolutePath}/%(title)s.%(ext)s")
            }

            YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, _ ->
                val normalizedPercent = (progress / 100f).coerceIn(0.0f, 1.0f)
                onProgress(
                    normalizedPercent,
                    0L,
                    0L,
                    0L,
                    etaInSeconds
                )
            }

            Result.success(taskId)
        } catch (e: Exception) {
            Result.failure(EngineException.DownloadFailed(e.message ?: "Download failed", e))
        }
    }

    open suspend fun pauseProcess(processId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val destroyed = YoutubeDL.getInstance().destroyProcessById(processId)
            if (destroyed) {
                Result.success(Unit)
            } else {
                Result.failure(EngineException.Cancelled("Process not found or already finished"))
            }
        } catch (e: Exception) {
            Result.failure(EngineException.Cancelled(e.message ?: "Failed to pause process", e))
        }
    }

    @Deprecated("Use pauseProcess(processId) instead")
    open suspend fun cancelDownload(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            YoutubeDL.getInstance().destroyProcessById("default")
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(EngineException.Cancelled("Cancel not supported by engine version", e))
        }
    }
}
