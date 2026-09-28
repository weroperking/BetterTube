package com.bettertube.app.domain.model

data class DetectedUrl(
    val url: String,
    val sourcePackage: String?,
    val detectionMethod: DetectionMethod
)

enum class DetectionMethod {
    CLIPBOARD,
    SHARE_INTENT,
    MANUAL_PASTE,
    PLATFORM_TILE
}
