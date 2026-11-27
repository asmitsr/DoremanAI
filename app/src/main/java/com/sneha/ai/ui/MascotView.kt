package com.sneha.ai.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.io.ByteArrayInputStream
import kotlin.math.abs

/** v2.3.0: animation states of the 3D Doreman mascot. */
enum class MascotState { IDLE, LISTENING, THINKING, SPEAKING, HAPPY, GREETING, SLEEPING }

private class MascotBridge(val onReady: () -> Unit, val onError: (String) -> Unit) {
    @JavascriptInterface
    fun onModelReady() {
        Handler(Looper.getMainLooper()).post { onReady() }
    }

    @JavascriptInterface
    fun onModelError(msg: String) {
        Handler(Looper.getMainLooper()).post { onError(msg) }
    }
}

/**
 * v2.3.0: renders the supplied Doreman_3D_Mascot.glb with a local Three.js
 * viewer (assets/mascot/) inside a WebView. Files are served through a
 * request interceptor on a fake https host, so ES modules and the GLB load
 * without network and without file:// CORS problems. If 3D fails for any
 * reason, the existing Orb is shown instead — the app keeps working.
 */
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun MascotView(
    state: MascotState,
    level: Float,
    mouthPulse: Long,
    reduceMotion: Boolean,
    phase: Phase,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ready by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var pageLoaded by remember { mutableStateOf(false) }

    Box(modifier) {
        if (!failed) {
            AndroidView(
                factory = { c ->
                    WebView(c).apply {
                        setBackgroundColor(Color.TRANSPARENT)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = false
                        settings.allowFileAccess = false
                        settings.cacheMode = WebSettings.LOAD_NO_CACHE
                        addJavascriptInterface(
                            MascotBridge({ ready = true }, { failed = true }),
                            "AndroidBridge"
                        )
                        webViewClient = object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val url = request?.url ?: return null
                                if (url.host != "mascot.local") return null
                                val path = url.path?.trimStart('/') ?: return null
                                if (path.contains("..")) return null
                                val mime = when {
                                    path.endsWith(".html") -> "text/html"
                                    path.endsWith(".js") -> "text/javascript"
                                    path.endsWith(".glb") -> "model/gltf-binary"
                                    else -> "application/octet-stream"
                                }
                                return try {
                                    if (path.endsWith(".glb")) {
                                        // The GLB ships base64-encoded (the push
                                        // tooling is text-only); decode on serve.
                                        val b64 = c.assets.open("mascot/$path.b64")
                                            .bufferedReader().use { it.readText() }
                                        val bytes = android.util.Base64.decode(
                                            b64.trim(), android.util.Base64.DEFAULT
                                        )
                                        WebResourceResponse(mime, "UTF-8", ByteArrayInputStream(bytes))
                                    } else {
                                        val stream = c.assets.open("mascot/$path")
                                        WebResourceResponse(mime, "UTF-8", stream)
                                    }
                                } catch (_: Exception) {
                                    WebResourceResponse(
                                        "text/plain", "UTF-8", 404, "Not Found",
                                        emptyMap(), ByteArrayInputStream(ByteArray(0))
                                    )
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                pageLoaded = true
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: android.webkit.WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) failed = true
                            }
                        }
                        // A short tap still triggers the mic (same as the old
                        // Orb); drags rotate the character in the viewer.
                        var downX = 0f
                        var downY = 0f
                        var downT = 0L
                        setOnTouchListener { _, ev ->
                            when (ev.actionMasked) {
                                MotionEvent.ACTION_DOWN -> {
                                    downX = ev.x; downY = ev.y; downT = ev.eventTime
                                }
                                MotionEvent.ACTION_UP -> {
                                    val moved = abs(ev.x - downX) + abs(ev.y - downY)
                                    if (moved < 40f && ev.eventTime - downT < 350L) onTap()
                                }
                            }
                            false
                        }
                        loadUrl("https://mascot.local/viewer.html")
                        webView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        if (!ready) {
            Orb(
                phase = phase,
                level = level,
                reduceMotion = reduceMotion,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE ->
                    webView?.evaluateJavascript("window.setPaused && window.setPaused(true)", null)
                Lifecycle.Event.ON_RESUME ->
                    webView?.evaluateJavascript("window.setPaused && window.setPaused(false)", null)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(obs)
            webView?.destroy()
            webView = null
        }
    }

    LaunchedEffect(state, pageLoaded) {
        if (pageLoaded) {
            webView?.evaluateJavascript(
                "window.setMascotState && window.setMascotState('${state.name.lowercase()}')", null
            )
        }
    }
    LaunchedEffect(reduceMotion, pageLoaded) {
        if (pageLoaded) {
            webView?.evaluateJavascript(
                "window.setReducedMotion && window.setReducedMotion($reduceMotion)", null
            )
        }
    }
    LaunchedEffect(level) {
        if (pageLoaded && state == MascotState.LISTENING) {
            webView?.evaluateJavascript("window.setMicLevel && window.setMicLevel($level)", null)
        }
    }
    LaunchedEffect(mouthPulse) {
        if (pageLoaded && mouthPulse > 0) {
            webView?.evaluateJavascript("window.mouthPulse && window.mouthPulse()", null)
        }
    }
}
