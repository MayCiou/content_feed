package com.example.content_feed.util

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HtmlStorageManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {

    companion object {
        private const val TAG = "HtmlStorageManager"
        private const val OFFLINE_DIR = "offline_articles"
        private const val ETAG_PREFIX = "etag_"
    }

    private val offlineArticlesDir: File
        get() {
            val dir = File(context.filesDir, OFFLINE_DIR)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    suspend fun downloadAndSaveHtml(
        articleId: Int,
        url: String
    ): String? {
        if (url.isBlank()) return null

        return withContext(Dispatchers.IO) {
            val targetFile =
                File(offlineArticlesDir, "article_$articleId.html")

            val etagFile =
                File(offlineArticlesDir, "$ETAG_PREFIX$articleId.txt")

            try {
                val requestBuilder = Request.Builder()
                    .url(url)
                    // Do NOT manually set Accept-Encoding.
                    // OkHttp will handle gzip/deflate transparently.
                    .header(
                        "Accept",
                        "text/html,application/xhtml+xml"
                    )
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Android; Mobile; rv:120.0) " +
                                "Gecko/120.0 Firefox/120.0"
                    )
                    .cacheControl(
                        CacheControl.Builder()
                            .maxStale(7, TimeUnit.DAYS)
                            .build()
                    )

                // Conditional request with ETag
                if (targetFile.exists() &&
                    targetFile.length() > 0 &&
                    etagFile.exists()
                ) {
                    val savedEtag = etagFile.readText().trim()

                    if (savedEtag.isNotBlank()) {
                        requestBuilder.header(
                            "If-None-Match",
                            savedEtag
                        )
                    }
                }

                val response = okHttpClient
                    .newCall(requestBuilder.build())
                    .execute()

                // 304 Not Modified
                if (response.code == 304 &&
                    targetFile.exists() &&
                    targetFile.length() > 0
                ) {
                    Log.d(
                        TAG,
                        "Article $articleId returned 304 Not Modified. " +
                                "Reusing local file."
                    )

                    response.close()
                    return@withContext targetFile.absolutePath
                }

                if (!response.isSuccessful) {
                    Log.w(
                        TAG,
                        "Failed to download HTML for article " +
                                "$articleId, code=${response.code}"
                    )

                    response.close()
                    return@withContext null
                }

                val responseBody =
                    response.body
                        ?: return@withContext null

                // OkHttp transparently decompresses gzip.
                targetFile.sink().buffer().use { sink ->
                    sink.writeAll(responseBody.source())
                }

                // Save ETag
                val newEtag = response.header("ETag")

                if (!newEtag.isNullOrBlank()) {
                    etagFile.writeText(newEtag)
                }

                Log.d(
                    TAG,
                    "HTML saved successfully for article " +
                            "$articleId (${targetFile.length()} bytes)"
                )

                targetFile.absolutePath

            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Error downloading HTML for article " +
                            "$articleId: ${e.message}",
                    e
                )

                if (targetFile.exists() &&
                    targetFile.length() > 0
                ) {
                    targetFile.absolutePath
                } else {
                    null
                }
            }
        }
    }

    suspend fun deleteSavedHtml(articleId: Int) {
        withContext(Dispatchers.IO) {
            try {
                val file = File(offlineArticlesDir, "article_$articleId.html")
                if (file.exists()) {
                    file.delete()
                }
                val etagFile = File(offlineArticlesDir, "$ETAG_PREFIX$articleId.txt")
                if (etagFile.exists()) {
                    etagFile.delete()
                }
                Log.d(TAG, "HTML and ETag cache deleted for article $articleId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting HTML for article $articleId: ${e.message}", e)
            }
        }
    }

    fun getLocalHtmlUriOrUrl(localHtmlPath: String?, originalUrl: String): String {
        if (!localHtmlPath.isNullOrBlank()) {
            val file = File(localHtmlPath)
            if (file.exists() && file.length() > 0) {
                return "file://${file.absolutePath}"
            }
        }
        return originalUrl
    }
}
