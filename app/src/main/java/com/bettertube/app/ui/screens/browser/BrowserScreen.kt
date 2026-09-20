package com.bettertube.app.ui.screens.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.utils.UrlValidator
import com.example.R
import kotlinx.coroutines.launch

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    initialUrl: String,
    navController: NavController,
    onDownloadDetected: (String) -> Unit,
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val decodedUrl = remember(initialUrl) {
        val decoded = Uri.decode(initialUrl)
        if (decoded.isNullOrBlank() || !UrlValidator.isValid(decoded)) null else decoded
    }

    LaunchedEffect(decodedUrl) {
        if (decodedUrl == null) {
            navController.popBackStack()
        }
    }

    if (decodedUrl == null) {
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val noUrlMessage = stringResource(R.string.no_url_in_clipboard)

    var webView: WebView? by remember { mutableStateOf(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    val host by viewModel.currentHost.collectAsStateWithLifecycle()

    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                CookieManager.getInstance().removeAllCookies(null)
                WebStorage.getInstance().deleteAllData()
                webView?.destroy()
                webView = null
            } catch (_: Exception) {
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (webView?.canGoBack() == true) {
                            webView?.goBack()
                        } else {
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.testTag("browser_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = host.ifEmpty { decodedUrl },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("browser_host_title")
                )

                IconButton(
                    onClick = {
                        val clipUrl = viewModel.getClipboardUrl()
                        if (clipUrl != null) {
                            onDownloadDetected(clipUrl)
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(noUrlMessage)
                            }
                        }
                    },
                    modifier = Modifier.testTag("browser_paste_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = stringResource(R.string.paste_url),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = true
                        settings.userAgentString = settings.userAgentString + " BetterTube/1.0"
                        settings.setSupportMultipleWindows(false)
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.setGeolocationEnabled(false)
                        settings.cacheMode = WebSettings.LOAD_DEFAULT

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val reqUrl = request?.url ?: return false
                                val scheme = reqUrl.scheme?.lowercase() ?: ""
                                if (scheme == "intent" || scheme == "market" || scheme == "tg") {
                                    return try {
                                        val intent = Intent.parseUri(reqUrl.toString(), Intent.URI_INTENT_SCHEME)
                                        ctx.startActivity(intent)
                                        true
                                    } catch (_: Exception) {
                                        true
                                    }
                                }
                                return false
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                hasError = false
                                canGoBack = view?.canGoBack() == true
                                url?.let {
                                    val uriHost = Uri.parse(it).host ?: ""
                                    viewModel.updateHost(uriHost)
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                canGoBack = view?.canGoBack() == true
                                url?.let {
                                    val uriHost = Uri.parse(it).host ?: ""
                                    viewModel.updateHost(uriHost)
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    hasError = true
                                }
                            }

                            @Deprecated("Deprecated in Java")
                            override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                super.onReceivedError(view, errorCode, description, failingUrl)
                                hasError = true
                            }
                        }

                        webChromeClient = object : WebChromeClient() {}

                        setDownloadListener { url, _, _, _, _ ->
                            if (!url.isNullOrBlank()) {
                                onDownloadDetected(url)
                            }
                        }

                        loadUrl(decodedUrl)
                        webView = this
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("browser_webview")
            )

            if (hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .testTag("browser_error_overlay"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.error_loading_page),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                hasError = false
                                webView?.reload()
                            },
                            modifier = Modifier.testTag("browser_retry_button")
                        ) {
                            Text(text = stringResource(R.string.retry))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BrowserScreenPreview() {
    BetterTubeTheme {
        BrowserScreen(
            initialUrl = "https://example.com",
            navController = rememberNavController(),
            onDownloadDetected = {}
        )
    }
}
