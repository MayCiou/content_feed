package com.example.content_feed.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.content_feed.data.model.ArticleItem
import com.example.content_feed.data.repository.ArticleRepository

class ArticlePagingSource(
    private val articleRepository: ArticleRepository
) : PagingSource<String, ArticleItem>() {

    companion object {
        const val PAGE_SIZE = 10
    }


    override fun getRefreshKey(
        state: PagingState<String, ArticleItem>
    ): String? {
        return null
    }

    override suspend fun load(
        params: LoadParams<String>
    ): LoadResult<String, ArticleItem> {

        val publishedAtLt = params.key

        return try {
            // Case A: Initial Load (publishedAtLt == null)
            // Can be either first launch (empty cache), cache expired (> 1 hour), or valid cache (< 1 hour)
            if (publishedAtLt == null) {
                val hasCached = articleRepository.hasCachedArticles()
                val isExpired = articleRepository.isCacheExpired()

                when {
                    // Scenario 1: First time launching the app with no cache in Room DB
                    !hasCached -> {
                        // Fetch initial articles from remote API and populate cache
                        val articles = articleRepository.getArticles(
                            limit = params.loadSize,
                            offset = 0,
                            publishedAtLt = null,
                            isRefresh = true
                        )
                        val nextKey = if (articles.isEmpty()) null else articleRepository.getOldestPublishedAt()

                        LoadResult.Page(
                            data = articles,
                            prevKey = null,
                            nextKey = nextKey
                        )
                    }

                    // Scenario 2: Cached data exists but is older than 1 hour (expired)
                    isExpired -> {
                        // Refresh latest articles from API, clear outdated cache, and reset refresh timestamp
                        val articles = articleRepository.getArticles(
                            limit = params.loadSize,
                            offset = 0,
                            publishedAtLt = null,
                            isRefresh = true
                        )
                        val nextKey = if (articles.isEmpty()) null else articleRepository.getOldestPublishedAt()

                        LoadResult.Page(
                            data = articles,
                            prevKey = null,
                            nextKey = nextKey
                        )
                    }

                    // Scenario 3: Valid cache available (less than 1 hour old)
                    else -> {
                        // Serve instantly from local database without network call
                        val localArticles = articleRepository.getCachedArticlesAsc()
                        val oldestPublishedAt = articleRepository.getOldestPublishedAt()

                        LoadResult.Page(
                            data = localArticles,
                            prevKey = null,
                            nextKey = oldestPublishedAt
                        )
                    }
                }
            } else {
                // Scenario 4: User scrolls down to fetch older articles (Append mode)
                // Fetch next page from remote API without clearing existing local cache
                val articles = articleRepository.getArticles(
                    limit = params.loadSize,
                    offset = 0,
                    publishedAtLt = publishedAtLt,
                    isRefresh = false
                )
                val nextKey = if (articles.isEmpty()) null else articleRepository.getOldestPublishedAt()

                LoadResult.Page(
                    data = articles,
                    prevKey = null,
                    nextKey = nextKey
                )
            }

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
