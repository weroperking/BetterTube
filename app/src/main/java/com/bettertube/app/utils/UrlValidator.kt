package com.bettertube.app.utils

import android.util.Patterns
import java.net.URI

object UrlValidator {
    private val URL_REGEX = Regex("https?://[^\\s]+")

    fun isValid(url: String): Boolean {
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            return false
        }
        val host = try {
            URI(trimmed).host
        } catch (_: Exception) {
            null
        }
        if (host.isNullOrBlank()) {
            return false
        }
        return try {
            Patterns.WEB_URL.matcher(trimmed).matches()
        } catch (_: Throwable) {
            true
        }
    }

    fun extractFirstUrl(text: String): String? {
        val match = URL_REGEX.find(text)
        return match?.value
    }

    fun matchesPlatform(url: String, platformName: String): Boolean {
        val host = try {
            URI(url).host?.lowercase() ?: ""
        } catch (_: Exception) {
            ""
        }
        if (host.isBlank()) return false

        return when (platformName) {
            "YouTube" -> host.contains("youtube.com") || host.contains("youtu.be")
            "Facebook" -> host.contains("facebook.com") || host.contains("fb.watch")
            "Instagram" -> host.contains("instagram.com")
            "Twitter" -> host.contains("twitter.com") || host.contains("x.com")
            "TikTok" -> host.contains("tiktok.com")
            "Vimeo" -> host.contains("vimeo.com")
            "Dailymotion" -> host.contains("dailymotion.com")
            "More" -> false
            else -> false
        }
    }
}
