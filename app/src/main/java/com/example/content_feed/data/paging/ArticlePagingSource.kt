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

            // Initial load
            if (publishedAtLt == null) {

                val shouldFetch =
                    articleRepository.shouldFetchFromApi()

                if (!shouldFetch) {

                    val localArticles =
                        articleRepository.getCachedArticlesAsc()

                    val oldestPublishedAt =
                        articleRepository.getOldestPublishedAt()

                    return LoadResult.Page(
                        data = localArticles,
                        prevKey = null,
                        nextKey = oldestPublishedAt
                    )
                }

                // Cache expired → fetch latest articles
                val articles = articleRepository.getArticles(
                    limit = params.loadSize,
                    offset = 0,
                    publishedAtLt = null,
                    isRefresh = true
                )

                val nextKey =
                    if (articles.isEmpty()) {
                        null
                    } else {
                        articleRepository.getOldestPublishedAt()
                    }

                LoadResult.Page(
                    data = articles,
                    prevKey = null,
                    nextKey = nextKey
                )

            } else {

                // Append → always fetch older articles
                val articles = articleRepository.getArticles(
                    limit = params.loadSize,
                    offset = 0,
                    publishedAtLt = publishedAtLt,
                    isRefresh = false
                )

                val nextKey =
                    if (articles.isEmpty()) {
                        null
                    } else {
                        articleRepository.getOldestPublishedAt()
                    }

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
