package com.bettertube.app.utils

import java.util.Locale

object FormatUtils {
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) {
            val formatted = String.format(Locale.US, "%.1f", kb)
            val clean = if (formatted.endsWith(".0")) formatted.substringBefore(".0") else formatted
            return "$clean KB"
        }
        val mb = kb / 1024.0
        if (mb < 1024.0) {
            val formatted = String.format(Locale.US, "%.1f", mb)
            val clean = if (formatted.endsWith(".0")) formatted.substringBefore(".0") else formatted
            return "$clean MB"
        }
        val gb = mb / 1024.0
        val formatted = String.format(Locale.US, "%.2f", gb)
        val clean = if (formatted.endsWith(".00")) {
            formatted.substringBefore(".00")
        } else if (formatted.endsWith("0")) {
            formatted.dropLast(1)
        } else {
            formatted
        }
        return "$clean GB"
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatBytes(bytesPerSecond)}/s"
    }

    fun formatEta(seconds: Long): String {
        if (seconds <= 0L) return "0s"
        return if (seconds < 60L) {
            "${seconds}s"
        } else if (seconds < 3600L) {
            val m = seconds / 60L
            val s = seconds % 60L
            "${m}m ${s}s"
        } else {
            val h = seconds / 3600L
            val m = (seconds % 3600L) / 60L
            "${h}h ${m}m"
        }
    }
}
