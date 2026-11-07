package com.padium.browser

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private var webView: WebView? = null
    private var cursorX by mutableFloatStateOf(0.5f)
    private var cursorY by mutableFloatStateOf(0.5f)
    private var leftX = 0f
    private var leftY = 0f
    private var rightY = 0f
    private var focusUrl by mutableStateOf(false)
    private var url by mutableStateOf("https://www.google.com")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var address by remember { mutableStateOf(url) }
            val go = {
                val text = address.trim()
                val target = if (text.startsWith("https://") || text.startsWith("http://")) text
                    else if (text.contains(".") && !text.contains(" ")) "https://$text"
                    else "https://www.google.com/search?q=" + java.net.URLEncoder.encode(text, "UTF-8")
                webView?.loadUrl(target)
            }
            MaterialTheme(colorScheme = darkColorScheme()) {
                BackHandler(webView?.canGoBack() == true) { webView?.goBack() }
                LaunchedEffect(Unit) {
                    while (true) {
                        if (abs(leftX) > 0.16f) cursorX = (cursorX + leftX * 0.012f).coerceIn(0f, 1f)
                        if (abs(leftY) > 0.16f) cursorY = (cursorY + leftY * 0.018f).coerceIn(0f, 1f)
                        if (abs(rightY) > 0.16f) webView?.scrollBy(0, (rightY * 25).toInt())
                        delay(16)
                    }
                }
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxWidth().padding(6.dp)) {
                        Button(onClick = { webView?.goBack() }) { Text("Back") }
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("URL or search") },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { go() })
                        )
                        Button(onClick = go) { Text("Go") }
                        Button(onClick = { webView?.reload() }) { Text("Reload") }
                    }
                    Box(Modifier.fillMaxSize()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    webChromeClient = WebChromeClient()
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, page: String?) {
                                            if (page != null) {
                                                url = page
                                                address = page
                                            }
                                        }
                                    }
                                    webView = this
                                    loadUrl(url)
                                }
                            }
                        )
                        Canvas(Modifier.fillMaxSize()) {
                            val center = Offset(cursorX * size.width, cursorY * size.height)
                            drawCircle(Color.Black, 12.dp.toPx(), center)
                            drawCircle(Color(0xFF55DDCC), 9.dp.toPx(), center, style = Stroke(3.dp.toPx()))
                        }
                    }
                }
            }
        }
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK) {
            leftX = event.getAxisValue(MotionEvent.AXIS_X)
            leftY = event.getAxisValue(MotionEvent.AXIS_Y)
            rightY = event.getAxisValue(MotionEvent.AXIS_RZ)
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> {
                webView?.let { view ->
                    val x = cursorX * view.width
                    val y = cursorY * view.height
                    val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, x, y, 0)
                    val up = MotionEvent.obtain(0, 20, MotionEvent.ACTION_UP, x, y, 0)
                    view.dispatchTouchEvent(down)
                    view.dispatchTouchEvent(up)
                    down.recycle()
                    up.recycle()
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_B -> {
                if (webView?.canGoBack() == true) webView?.goBack() else finish()
                return true
            }
            KeyEvent.KEYCODE_BUTTON_Y -> { webView?.reload(); return true }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }
}
