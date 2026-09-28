package com.bettertube.app.domain.model

data class MediaFormat(
    val formatId: String,
    val extension: String,
    val resolution: String?,
    val audioBitrate: Int?,
    val videoBitrate: Int?,
    val fileSizeBytes: Long?,
    val note: String,
    val isVideo: Boolean,
    val isAudio: Boolean
)
