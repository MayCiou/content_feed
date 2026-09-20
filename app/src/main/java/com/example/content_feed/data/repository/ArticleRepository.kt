package com.example.content_feed.data.repository

import android.util.Log
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.content_feed.data.local.ArticleRefreshDao
import com.example.content_feed.data.local.ArticleRefreshEntity
import com.example.content_feed.data.model.ArticleItem
import com.example.content_feed.data.paging.ArticlePagingSource
import com.example.content_feed.data.remote.SpaceflightApiService
import com.example.content_feed.data.remote.model.ArticleDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class ArticleRepository @Inject constructor(
    private val spaceflightApiService: SpaceflightApiService,
    private val articleRefreshDao: ArticleRefreshDao
) {

    companion object {
        private const val TAG = "ArticleRepository"
        private const val DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss"
        private const val MIN_REQUEST_INTERVAL_MS = 100L
        private const val MAX_429_RETRIES = 3

        private const val DEFAULT_RETRY_AFTER_MS = 1500L
    }

    private val apiMutex = Mutex()
    private var lastRequestTimestamp = 0L

    fun getArticlesStream(): Flow<PagingData<ArticleItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = ArticlePagingSource.PAGE_SIZE,
                initialLoadSize = ArticlePagingSource.PAGE_SIZE,
                enablePlaceholders = false,
                prefetchDistance = 10
            ),
            pagingSourceFactory = { ArticlePagingSource(this) }
        ).flow
    }

    suspend fun getArticles(
        limit: Int = ArticlePagingSource.PAGE_SIZE,
        offset: Int = 0
    ): List<ArticleItem> {
        return withContext(Dispatchers.IO) {

            var lastException: Exception? = null

            repeat(MAX_429_RETRIES + 1) { attempt ->

                try {
                    val response = apiMutex.withLock {
                        throttleRequestInterval()

                        Log.d(TAG, "Requesting articles: limit=$limit, offset=$offset")

                        spaceflightApiService.getArticles(
                            limit = limit,
                            offset = offset
                        )
                    }

                    Log.d(TAG, "Articles loaded: limit=$limit, offset=$offset")

                    val articles = response.results.map { dto ->
                        dto.toArticleItem()
                    }

                    saveLastRefreshTimeAsync()

                    return@withContext articles

                } catch (e: CancellationException) {
                    throw e

                } catch (e: HttpException) {

                    lastException = e

                    if (e.code() != 429 || attempt >= MAX_429_RETRIES) {
                        throw e
                    }

                    val backoffDelay =
                        1000L * (1L shl attempt)

                    val retryAfterMs =
                        e.response()
                            ?.headers()
                            ?.get("Retry-After")
                            ?.toLongOrNull()
                            ?.times(1000L)
                            ?: backoffDelay.coerceAtLeast(
                                DEFAULT_RETRY_AFTER_MS
                            )

                    Log.w(
                        TAG,
                        "HTTP 429. Retrying " +
                                "${attempt + 1}/$MAX_429_RETRIES " +
                                "after ${retryAfterMs}ms..."
                    )

                    delay(retryAfterMs)
                }
            }

            throw lastException
                ?: IllegalStateException("Unexpected API retry termination")
        }
    }

    private suspend fun saveLastRefreshTimeAsync() {
        try {
            val currentTime = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault())
            val formattedTime = dateFormat.format(Date(currentTime))
            articleRefreshDao.insertOrUpdateArticleRefresh(
                ArticleRefreshEntity(id = 1, lastArticleRefreshTime = formattedTime)
            )
        } catch (e: CancellationException) {
            throw e
        } catch (ignored: Exception) {
        }
    }

    private suspend fun throttleRequestInterval() {
        val now = System.currentTimeMillis()
        val timeSinceLastRequest = now - lastRequestTimestamp
        if (timeSinceLastRequest < MIN_REQUEST_INTERVAL_MS) {
            delay(MIN_REQUEST_INTERVAL_MS - timeSinceLastRequest)
        }
        lastRequestTimestamp = System.currentTimeMillis()
    }

    private fun ArticleDto.toArticleItem(): ArticleItem {
        return ArticleItem(
            id = id,
            imageUrl = imageUrl.orEmpty(),
            title = title,
            publishedDate = formatPublishedDate(publishedAt),
            isSaved = isSaved
        )
    }

    private fun formatPublishedDate(isoDateString: String?): String {
        if (isoDateString.isNullOrBlank()) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(isoDateString)
            if (date != null) {
                val outputFormat = SimpleDateFormat("M/d", Locale.getDefault())
                outputFormat.format(date)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
