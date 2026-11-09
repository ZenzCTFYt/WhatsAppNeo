package com.whatsapp.neo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var splash: android.view.View

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uris: Array<Uri>? = if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { arrayOf(it) }
                ?: result.data?.clipData?.let { cd ->
                    (0 until cd.itemCount).map { cd.getItemAt(it).uri }.toTypedArray()
                }
        } else null
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        splash = findViewById(R.id.splash)
        webView.setBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))

        setupWebView()

        findViewById<android.widget.Button>(R.id.btnZoomOut).setOnClickListener { webView.zoomBy(0.8f) }
        findViewById<android.widget.Button>(R.id.btnZoomIn).setOnClickListener { webView.zoomBy(1.25f) }
        findViewById<android.widget.Button>(R.id.btnZoomFit).setOnClickListener {
            webView.setInitialScale(0)
            webView.reload()
        }

        requestRuntimePermissions()

        webView.loadUrl("https://web.whatsapp.com")
    }

    override fun onSaveInstanceState(outState: android.os.Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    private fun setupWebView() {
        val s = webView.settings
        s.javaScriptEnabled = true
        s.domStorageEnabled = true
        s.databaseEnabled = true
        s.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        s.allowFileAccess = true
        s.allowContentAccess = true
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        s.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.setRenderPriority(WebSettings.RenderPriority.HIGH)
        s.setLoadsImagesAutomatically(true)
        s.setBlockNetworkImage(false)
        webView.setInitialScale(0)
        webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        webView.overScrollMode = android.view.View.OVER_SCROLL_NEVER
        webView.scrollBarStyle = android.view.View.SCROLLBARS_INSIDE_OVERLAY
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                splash.visibility = android.view.View.GONE
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
                super.onReceivedError(view, request, error)
                splash.visibility = android.view.View.GONE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }

        // Fallback: hide splash even if page never finishes
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            splash.visibility = android.view.View.GONE
        }, 8000)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                this@MainActivity.filePathCallback = filePathCallback
                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (fileChooserParams?.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                filePicker.launch(intent)
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.CAMERA)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.RECORD_AUDIO)
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
