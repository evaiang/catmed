package com.yy.catmed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.yy.catmed.web.WebAppInterface

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.yy.catmed.REFRESH_DATA") {
                runOnUiThread {
                    webView.evaluateJavascript("if(window.refreshData) refreshData();", null)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        setupWebView()

        // 注册数据刷新广播（来自悬浮窗记录后通知UI刷新）
        registerReceiver(refreshReceiver, IntentFilter("com.yy.catmed.REFRESH_DATA"),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) RECEIVER_EXPORTED else 0
        )
    }

    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            useWideViewPort = true
            builtInZoomControls = false
            setSupportZoom(false)
        }

        webView.addJavascriptInterface(WebAppInterface(this), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean = false
        }

        webView.webChromeClient = WebChromeClient()

        // 从 assets 加载 HTML
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(refreshReceiver)
        } catch (_: Exception) {}
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
