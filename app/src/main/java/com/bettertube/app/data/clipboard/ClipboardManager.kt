package com.bettertube.app.data.clipboard

import android.content.ClipDescription
import android.content.Context
import com.bettertube.app.utils.UrlValidator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClipboardManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun getClipboardText(): String? {
        return try {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                ?: return null
            if (!cm.hasPrimaryClip()) return null
            val clip = cm.primaryClip ?: return null
            if (clip.itemCount == 0) return null

            val description = clip.description
            if (description != null && !description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) && !description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)) {
                // Non-text data
                return null
            }

            val item = clip.getItemAt(0) ?: return null
            val text = item.text?.toString() ?: item.coerceToText(context)?.toString()
            if (text.isNullOrBlank()) null else text
        } catch (_: Exception) {
            null
        }
    }

    fun hasNewUrl(lastSeen: String?): String? {
        val text = getClipboardText() ?: return null
        val extractedUrl = UrlValidator.extractFirstUrl(text) ?: return null
        return if (extractedUrl != lastSeen) extractedUrl else null
    }
}
