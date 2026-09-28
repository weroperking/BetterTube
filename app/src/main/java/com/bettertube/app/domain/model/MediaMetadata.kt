package com.bettertube.app.domain.model

data class MediaMetadata(
    val id: String,
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val webpageUrl: String,
    val formats: List<MediaFormat>,
    val extractor: String
)
