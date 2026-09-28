package com.bettertube.app.data.share

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShareIntentHandlerTest {

    private lateinit var handler: ShareIntentHandler

    @Before
    fun setUp() {
        handler = ShareIntentHandler()
    }

    @Test
    fun handleIntent_returnsUrl_whenActionSendWithTextPlain() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Watch this: https://youtube.com/watch?v=abc")
        }
        val result = handler.handleIntent(intent)
        assertEquals("https://youtube.com/watch?v=abc", result)
    }

    @Test
    fun handleIntent_returnsNull_whenActionSendWithEmptyText() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "")
        }
        val result = handler.handleIntent(intent)
        assertNull(result)
    }

    @Test
    fun handleIntent_returnsUrl_whenActionViewWithValidUrl() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://youtube.com/watch?v=xyz")
        }
        val result = handler.handleIntent(intent)
        assertEquals("https://youtube.com/watch?v=xyz", result)
    }

    @Test
    fun handleIntent_returnsNull_whenActionIsNull() {
        val intent = Intent()
        val result = handler.handleIntent(intent)
        assertNull(result)
    }
}
