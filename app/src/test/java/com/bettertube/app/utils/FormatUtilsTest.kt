package com.bettertube.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun formatBytes_returnsB_forValuesUnder1024() {
        assertEquals("0 B", FormatUtils.formatBytes(0L))
        assertEquals("500 B", FormatUtils.formatBytes(500L))
        assertEquals("1023 B", FormatUtils.formatBytes(1023L))
    }

    @Test
    fun formatBytes_returnsKB_forValuesUnder1MB() {
        val result = FormatUtils.formatBytes(1024L)
        assertTrue(result.contains("KB"))
        assertEquals("1 KB", result)

        val result2 = FormatUtils.formatBytes(1229L)
        assertEquals("1.2 KB", result2)
    }

    @Test
    fun formatBytes_returnsMB_forValuesUnder1GB() {
        val bytes = (45.3 * 1024 * 1024).toLong()
        val result = FormatUtils.formatBytes(bytes)
        assertTrue(result.contains("MB"))
        assertEquals("45.3 MB", result)
    }

    @Test
    fun formatBytes_returnsGB_forValuesOver1GB() {
        val bytes = (1.02 * 1024 * 1024 * 1024).toLong()
        val result = FormatUtils.formatBytes(bytes)
        assertTrue(result.contains("GB"))
        assertEquals("1.02 GB", result)
    }

    @Test
    fun formatEta_returnsSeconds_forValuesUnder60() {
        assertEquals("0s", FormatUtils.formatEta(0L))
        assertEquals("45s", FormatUtils.formatEta(45L))
        assertEquals("59s", FormatUtils.formatEta(59L))
    }

    @Test
    fun formatEta_returnsMinutesAndSeconds_forValuesUnder3600() {
        assertEquals("1m 0s", FormatUtils.formatEta(60L))
        assertEquals("2m 15s", FormatUtils.formatEta(135L))
        assertEquals("59m 59s", FormatUtils.formatEta(3599L))
    }

    @Test
    fun formatEta_returnsHoursAndMinutes_forValuesOver3600() {
        assertEquals("1h 0m", FormatUtils.formatEta(3600L))
        assertEquals("1h 30m", FormatUtils.formatEta(5400L))
        assertEquals("2h 15m", FormatUtils.formatEta(8100L))
    }
}
