package com.bettertube.app.domain.model

enum class DownloadStatus {
    QUEUED,
    WAITING,
    EXTRACTING,
    DOWNLOADING,
    MERGING,
    COMPLETE,
    PAUSED,
    FAILED,
    CANCELLED
}
