package com.bettertube.app.data.share

import android.content.Intent
import com.bettertube.app.utils.UrlValidator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareIntentHandler @Inject constructor() {

    fun handleIntent(intent: Intent?): String? {
        if (intent == null) return null

        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (text.isNullOrBlank()) return null
            return UrlValidator.extractFirstUrl(text)
        }

        if (intent.action == Intent.ACTION_VIEW) {
            val dataString = intent.data?.toString()
            if (!dataString.isNullOrBlank() && UrlValidator.isValid(dataString)) {
                return dataString
            }
        }

        return null
    }
}
