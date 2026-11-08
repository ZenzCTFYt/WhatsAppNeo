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
        val uris = result.data?.data?.let { arrayOf(it) }
            ?: result.data?.clipData?.let { cd ->
                (0 until cd.itemCount).map { cd.getItemAt(it).uri }.toTypedArray()
            }
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        splash = findViewById(R.id.splash)
        webView.setBackgroundColor(android.graphics.Color.parseColor("#F5F0E6"))

        setupWebView()

        findViewById<android.widget.Button>(R.id.btnZoomIn).setOnClickListener { webView.zoomBy(1.25f) }
        findViewById<android.widget.Button>(R.id.btnZoomOut).setOnClickListener { webView.zoomBy(0.8f) }
        findViewById<android.widget.Button>(R.id.btnZoomReset).setOnClickListener { webView.reload() }

        requestRuntimePermissions()

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl("https://web.whatsapp.com")
        }
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
        s.cacheMode = WebSettings.LOAD_DEFAULT
        s.allowFileAccess = false
        s.allowContentAccess = true
        s.mediaPlaybackRequiresUserGesture = false
        s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        s.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.setSupportZoom(true)
        s.builtInZoomControls = true
        s.displayZoomControls = false
        s.setInitialScale(0)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectTheme(view)
                splash.visibility = android.view.View.GONE
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
                super.onReceivedError(view, request, error)
                splash.visibility = android.view.View.GONE
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    view?.reload()
                }, 2000)
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
                filePicker.launch(intent)
                return true
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }
    }

    private fun injectTheme(view: WebView?) {
        val css = assets.open("neo-brutalism.css").bufferedReader().use { it.readText() }
        val escaped = css
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
        val js = """
            (function() {
              var id = 'neo-brutalism-theme';
              var old = document.getElementById(id);
              if (old) old.remove();
              var st = document.createElement('style');
              st.id = id;
              st.textContent = '$escaped';
              (document.head || document.documentElement).appendChild(st);
              // Responsive viewport
              var meta = document.querySelector('meta[name="viewport"]');
              if (!meta) { meta = document.createElement('meta'); meta.name = 'viewport'; (document.head || document.documentElement).appendChild(meta); }
              meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes';
              if (!window.__nbObs) {
                window.__nbObs = new MutationObserver(function() {
                  if (!document.getElementById(id)) {
                    var s = document.createElement('style');
                    s.id = id;
                    s.textContent = '$escaped';
                    (document.head || document.documentElement).appendChild(s);
                  }
                });
                window.__nbObs.observe(document.documentElement, { childList: true, subtree: true });
              }
            })();
        """.trimIndent()
        view?.evaluateJavascript(js, null)
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
