package com.bettertube.app.domain.model

data class Aria2Download(
    val gid: String,
    val name: String,
    val status: Aria2DownloadStatus,
    val totalBytes: Long,
    val completedBytes: Long,
    val downloadSpeed: Long,
    val uploadSpeed: Long,
    val connections: Int,
    val seeders: Int,
    val isTorrent: Boolean,
    val isMetalink: Boolean,
    val errorMessage: String?
)

enum class Aria2DownloadStatus {
    ACTIVE,
    WAITING,
    PAUSED,
    COMPLETE,
    ERROR,
    REMOVED;

    companion object {
        fun fromString(status: String): Aria2DownloadStatus {
            return when (status.lowercase()) {
                "active" -> ACTIVE
                "waiting" -> WAITING
                "paused" -> PAUSED
                "complete" -> COMPLETE
                "error" -> ERROR
                "removed" -> REMOVED
                else -> WAITING
            }
        }
    }
}
