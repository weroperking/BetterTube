package com.bettertube.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UrlValidatorTest {

    @Test
    fun isValid_returnsTrue_forHttpsUrl() {
        val result = UrlValidator.isValid("https://www.youtube.com/watch?v=12345")
        assertTrue(result)
    }

    @Test
    fun isValid_returnsFalse_forBlankString() {
        val result = UrlValidator.isValid("   ")
        assertFalse(result)
    }

    @Test
    fun isValid_returnsFalse_forStringWithoutScheme() {
        val result = UrlValidator.isValid("youtube.com/watch?v=12345")
        assertFalse(result)
    }

    @Test
    fun extractFirstUrl_returnsFirstUrl_fromTextWithMultipleUrls() {
        val text = "Check out https://youtube.com/watch?v=123 and also https://vimeo.com/456"
        val result = UrlValidator.extractFirstUrl(text)
        assertEquals("https://youtube.com/watch?v=123", result)
    }

    @Test
    fun extractFirstUrl_returnsNull_whenNoUrlPresent() {
        val text = "Just some text without any web links present."
        val result = UrlValidator.extractFirstUrl(text)
        assertNull(result)
    }

    @Test
    fun matchesPlatform_returnsTrue_forYouTubeUrl() {
        val result1 = UrlValidator.matchesPlatform("https://www.youtube.com/watch?v=123", "YouTube")
        val result2 = UrlValidator.matchesPlatform("https://youtu.be/123", "YouTube")
        assertTrue(result1)
        assertTrue(result2)
    }

    @Test
    fun matchesPlatform_returnsFalse_forUnrelatedUrl() {
        val result = UrlValidator.matchesPlatform("https://example.com/media/file", "YouTube")
        assertFalse(result)
    }
}
