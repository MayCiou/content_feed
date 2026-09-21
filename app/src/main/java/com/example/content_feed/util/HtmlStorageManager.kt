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

    suspend fun downloadAndSaveHtml(articleId: Int, url: String): String? {
        if (url.isBlank()) return null
        return withContext(Dispatchers.IO) {
            val targetFile = File(offlineArticlesDir, "article_$articleId.html")
            val etagFile = File(offlineArticlesDir, "$ETAG_PREFIX$articleId.txt")

            try {
                val requestBuilder = Request.Builder()
                    .url(url)
                    // 1. 啟用 gzip/deflate 壓縮 (省 70%~80% 流量)
                    .header("Accept-Encoding", "gzip, deflate")
                    // 2. 僅接受 text/html 網頁內文，排除非必要廣告與資源
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0")
                    .cacheControl(CacheControl.Builder().maxStale(7, TimeUnit.DAYS).build())

                // 3. 條件式請求 (If-None-Match ETag 快取協定)
                if (targetFile.exists() && targetFile.length() > 0 && etagFile.exists()) {
                    val savedEtag = etagFile.readText().trim()
                    if (savedEtag.isNotBlank()) {
                        requestBuilder.header("If-None-Match", savedEtag)
                    }
                }

                val response = okHttpClient.newCall(requestBuilder.build()).execute()

                // 304 Not Modified: 遠端內容無異動，直接沿用本地檔案，消耗 0 流量！
                if (response.code == 304 && targetFile.exists() && targetFile.length() > 0) {
                    Log.d(TAG, "Article $articleId returned 304 Not Modified. Reusing local file (0 network used).")
                    response.close()
                    return@withContext targetFile.absolutePath
                }

                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to download HTML for article $articleId, code: ${response.code}")
                    response.close()
                    return@withContext null
                }

                val responseBody = response.body ?: return@withContext null

                // 串流式寫入磁碟 (Streaming via okio BufferedSink) - 杜絕全量記憶體載入造成 OOM
                targetFile.sink().buffer().use { sink ->
                    sink.writeAll(responseBody.source())
                }

                // 儲存 ETag 標頭供下次比對
                val newEtag = response.header("ETag")
                if (!newEtag.isNullOrBlank()) {
                    etagFile.writeText(newEtag)
                }

                Log.d(TAG, "HTML streamed successfully for article $articleId (${targetFile.length()} bytes)")
                targetFile.absolutePath
            } catch (e: Exception) {
                Log.e(TAG, "Error streaming HTML for article $articleId: ${e.message}", e)
                if (targetFile.exists() && targetFile.length() > 0) {
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
