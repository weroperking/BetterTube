package com.bettertube.app.domain.repository

import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.domain.model.ExtractionPreset
import com.bettertube.app.domain.model.MediaMetadata
import com.bettertube.app.domain.model.PlaylistInfo
import com.bettertube.app.domain.model.SubtitleTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface DownloadRepository {
    suspend fun fetchMetadata(url: String): Result<MediaMetadata>
    suspend fun fetchPlaylist(url: String): Result<PlaylistInfo>
    suspend fun startPlaylistDownload(
        url: String,
        selectedIndices: List<Int>,
        formatId: String,
        preset: ExtractionPreset,
        downloadSubtitles: Boolean,
        subtitleLanguages: List<String>,
        embedSubtitles: Boolean
    ): Result<List<String>>
    suspend fun fetchSubtitles(url: String): Result<List<SubtitleTrack>>
    suspend fun startDownload(task: DownloadTask): Result<String>
    suspend fun pauseDownload(id: String): Result<Unit>
    suspend fun resumeDownload(id: String): Result<Unit>
    suspend fun cancelDownload(id: String): Result<Unit>
    suspend fun setDownloadSpeedLimit(id: String, bytesPerSecond: Long?): Result<Unit>
    suspend fun setGlobalSpeedLimit(bytesPerSecond: Long?): Result<Unit>
    suspend fun reorderQueue(orderedIds: List<String>): Result<Unit>
    suspend fun retryDownload(id: String): Result<Unit>
    fun getGlobalSpeedLimit(): StateFlow<Long?>
    fun getWifiOnly(): StateFlow<Boolean>
    suspend fun setWifiOnly(enabled: Boolean): Result<Unit>
    fun getAllTasks(): Flow<List<DownloadTask>>
    fun getTask(id: String): Flow<DownloadTask?>
}

