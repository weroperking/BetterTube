package com.bettertube.app.domain.model

data class PlaylistInfo(
    val id: String,
    val title: String,
    val uploader: String,
    val entryCount: Int,
    val entries: List<PlaylistEntry>,
    val extractor: String
)

data class PlaylistEntry(
    val index: Int,
    val id: String,
    val title: String,
    val durationSeconds: Long,
    val thumbnailUrl: String,
    val url: String
)
