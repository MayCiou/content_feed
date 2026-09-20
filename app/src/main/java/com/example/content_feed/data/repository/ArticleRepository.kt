package com.example.content_feed.data.repository

import com.example.content_feed.data.local.ArticleRefreshDao
import com.example.content_feed.data.local.ArticleRefreshEntity
import com.example.content_feed.data.model.ArticleItem
import com.example.content_feed.data.remote.SpaceflightApiService
import com.example.content_feed.data.remote.model.ArticleDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        private const val DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss"
    }

    suspend fun getArticles(limit: Int = 10, offset: Int = 0): List<ArticleItem> {
        return withContext(Dispatchers.IO) {
            val response = spaceflightApiService.getArticles(limit = limit, offset = offset)

            val currentTime = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault())
            val formattedTime = dateFormat.format(Date(currentTime))
            articleRefreshDao.insertOrUpdateArticleRefresh(
                ArticleRefreshEntity(id = 1, lastArticleRefreshTime = formattedTime)
            )

            response.results.map { dto ->
                dto.toArticleItem()
            }
        }
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
