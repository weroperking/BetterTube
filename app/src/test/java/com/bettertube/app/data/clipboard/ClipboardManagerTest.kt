package com.bettertube.app.data.clipboard

import android.content.ClipData
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClipboardManagerTest {

    private lateinit var context: Context
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var systemClipboard: android.content.ClipboardManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clipboardManager = ClipboardManager(context)
        systemClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        systemClipboard.clearPrimaryClip()
    }

    @Test
    fun getClipboardText_returnsNull_whenClipboardEmpty() {
        val result = clipboardManager.getClipboardText()
        assertNull(result)
    }

    @Test
    fun getClipboardText_returnsText_whenClipboardHasText() {
        val clip = ClipData.newPlainText("label", "https://youtube.com/watch?v=123")
        systemClipboard.setPrimaryClip(clip)

        val result = clipboardManager.getClipboardText()
        assertEquals("https://youtube.com/watch?v=123", result)
    }
}
