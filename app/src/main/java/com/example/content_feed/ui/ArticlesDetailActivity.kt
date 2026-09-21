package com.example.content_feed.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.content_feed.R
import com.example.content_feed.databinding.ActivityArticlesDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class ArticlesDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_LOCAL_HTML_PATH = "extra_local_html_path"
        const val EXTRA_LOAD_MODE = "extra_load_mode"

        const val MODE_URL = "mode_url"
        const val MODE_HTML = "mode_html"
    }

    private lateinit var binding: ActivityArticlesDetailBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        enableEdgeToEdge()
        binding = ActivityArticlesDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.mainDetailLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val localHtmlPath = intent.getStringExtra(EXTRA_LOCAL_HTML_PATH).orEmpty()
        val loadMode = intent.getStringExtra(EXTRA_LOAD_MODE) ?: MODE_URL

        binding.btnBack.setOnClickListener {
            finishActivityWithAnimation()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webViewArticles.canGoBack()) {
                    binding.webViewArticles.goBack()
                } else {
                    finishActivityWithAnimation()
                }
            }
        })

        setupWebView()
        loadContent(loadMode, url, localHtmlPath)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = binding.webViewArticles.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.allowFileAccessFromFileURLs = true
        settings.allowUniversalAccessFromFileURLs = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.builtInZoomControls = true
        settings.displayZoomControls = false

        binding.webViewArticles.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                binding.pbWebLoading.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                binding.pbWebLoading.visibility = View.GONE
            }
        }

        binding.webViewArticles.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress >= 100) {
                    binding.pbWebLoading.visibility = View.GONE
                }
            }
        }
    }

    private fun loadContent(
        mode: String,
        url: String,
        localHtmlPath: String
    ) {
        val tag = "WebView"

        Log.d(
            tag,
            "loadContent: mode=$mode, url=$url, localHtmlPath=$localHtmlPath"
        )

        if (mode == MODE_HTML) {
            val localFile =
                if (localHtmlPath.isNotBlank()) {
                    File(localHtmlPath)
                } else {
                    null
                }

            if (localFile != null &&
                localFile.exists() &&
                localFile.length() > 0
            ) {
                Log.d(tag, "Local HTML exists")
                Log.d(tag, "Local HTML path=${localFile.absolutePath}")
                Log.d(tag, "Local HTML size=${localFile.length()} bytes")

                try {
                    val htmlContent = localFile.readText(Charsets.UTF_8)

                    Log.d(
                        tag,
                        "HTML decoded length=${htmlContent.length} chars"
                    )

                    Log.d(
                        tag,
                        "HTML preview=${htmlContent.take(300)}"
                    )

                    val baseUrl = if (url.isNotBlank()) {
                        url
                    } else {
                        "https://localhost/"
                    }

                    Log.d(
                        tag,
                        "Loading local HTML with baseUrl=$baseUrl"
                    )

                    binding.webViewArticles.loadDataWithBaseURL(
                        baseUrl,
                        htmlContent,
                        "text/html",
                        "UTF-8",
                        null
                    )

                } catch (e: Exception) {
                    Log.e(
                        tag,
                        "Failed to read local HTML, " +
                                "fallback to file URL",
                        e
                    )

                    val fileUrl = localFile.toURI().toString()

                    Log.d(
                        tag,
                        "Fallback file URL=$fileUrl"
                    )

                    binding.webViewArticles.loadUrl(fileUrl)
                }
            } else {
                Log.d(
                    tag,
                    "Local HTML not available: " +
                            "exists=${localFile?.exists()}, " +
                            "size=${localFile?.length() ?: 0}"
                )

                if (url.isNotBlank()) {
                    Log.d(tag, "Loading remote URL=$url")
                    binding.webViewArticles.loadUrl(url)
                } else {
                    Log.w(tag, "No local HTML and no URL")
                }
            }
        } else {
            if (url.isNotBlank()) {
                Log.d(
                    tag,
                    "Non-HTML mode, loading URL=$url"
                )

                binding.webViewArticles.loadUrl(url)
            } else {
                Log.w(
                    tag,
                    "Non-HTML mode but URL is empty"
                )
            }
        }
    }

    private fun finishActivityWithAnimation() {
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    override fun onDestroy() {
        binding.webViewArticles.stopLoading()
        binding.webViewArticles.destroy()
        super.onDestroy()
    }
}
