package com.bettertube.app.ui.screens.browser

import androidx.lifecycle.ViewModel
import com.bettertube.app.data.clipboard.ClipboardManager
import com.bettertube.app.utils.UrlValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val clipboardManager: ClipboardManager
) : ViewModel() {

    val currentHost = MutableStateFlow("")

    fun updateHost(host: String) {
        currentHost.value = host
    }

    fun getClipboardUrl(): String? {
        val text = clipboardManager.getClipboardText() ?: return null
        return UrlValidator.extractFirstUrl(text)
    }
}
