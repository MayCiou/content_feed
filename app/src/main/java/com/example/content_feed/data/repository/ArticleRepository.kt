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

    suspend fun getArticles(limit: Int = ArticlePagingSource.PAGE_SIZE, offset: Int = 0): List<ArticleItem> {
        return withContext(Dispatchers.IO) {
            apiMutex.withLock {
                throttleRequestInterval()

                var retryCount = 0
                var backoffDelay = 1000L
                var articles: List<ArticleItem>? = null

                while (articles == null) {
                    try {
                        val response = spaceflightApiService.getArticles(limit = limit, offset = offset)
                        Log.d(TAG, "Articles loaded: limit=$limit, offset=$offset")

                        articles = response.results.map { dto ->
                            dto.toArticleItem()
                        }
                    } catch (e: Exception) {
                        if (e is HttpException && e.code() == 429 && retryCount < MAX_429_RETRIES) {
                            retryCount++
                            Log.w(TAG, "Rate limited (HTTP 429). Retrying attempt $retryCount after ${backoffDelay}ms...")
                            delay(backoffDelay)
                            backoffDelay *= 2
                        } else {
                            throw e
                        }
                    }
                }

                // Async update refresh time to DB without blocking return
                saveLastRefreshTimeAsync()

                articles
            }
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
