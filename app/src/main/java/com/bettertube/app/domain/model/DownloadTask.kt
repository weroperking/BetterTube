package com.bettertube.app.domain.model

data class DownloadTask(
    val id: String,
    val url: String,
    val title: String,
    val thumbnailUrl: String,
    val formatId: String,
    val status: DownloadStatus,
    val progressPercent: Float,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val speedBytesPerSecond: Long,
    val etaSeconds: Long,
    val outputFilePath: String?,
    val errorMessage: String?,
    val createdAtMillis: Long,
    val mediaType: MediaType = MediaType.OTHER,
    val processId: String? = null,
    val lastAttemptUrl: String? = null,
    val retryCount: Int = 0,
    val speedLimitBytesPerSecond: Long? = null,
    val preset: ExtractionPreset = ExtractionPreset.VIDEO_ORIGINAL,
    val downloadSubtitles: Boolean = false,
    val subtitleLanguages: List<String> = emptyList(),
    val embedSubtitles: Boolean = false,
    val isPlaylistChild: Boolean = false,
    val playlistParentUrl: String? = null
)
