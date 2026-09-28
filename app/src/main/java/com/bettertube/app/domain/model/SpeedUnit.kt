package com.bettertube.app.domain.model

enum class SpeedUnit(val bytesPerSecond: Long) {
    KBPS(1024L),
    MBPS(1024L * 1024L),
    UNLIMITED(0L)
}
