package com.example.content_feed.data.paging

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.content_feed.data.model.ArticleItem
import com.example.content_feed.data.repository.ArticleRepository

class ArticlePagingSource(
    private val articleRepository: ArticleRepository
) : PagingSource<Int, ArticleItem>() {

    companion object {
        const val PAGE_SIZE = 20
    }

    private var isUsingLocalCache = false

    override fun getRefreshKey(state: PagingState<Int, ArticleItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(PAGE_SIZE)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(PAGE_SIZE)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ArticleItem> {
        val offset = params.key ?: 0
        return try {
            // 首次載入 (offset == 0) 時檢查是否小於 1 小時且有快取
            if (offset == 0) {
                val shouldFetch = articleRepository.shouldFetchFromApi()
                if (!shouldFetch) {
                    val localArticles = articleRepository.getCachedArticlesAsc()
                    if (localArticles.isNotEmpty()) {
                        isUsingLocalCache = true
                        return LoadResult.Page(
                            data = localArticles,
                            prevKey = null,
                            nextKey = localArticles.size
                        )
                    }
                }
                isUsingLocalCache = false
            }

            // 若之前使用了 local cache，向下滑到接近底部觸發 append 時，使用最舊的 publishedAt 帶入 published_at_lt
            val articles = if (isUsingLocalCache) {
                val oldestTime = articleRepository.getOldestPublishedAt()
                articleRepository.getArticles(
                    limit = params.loadSize,
                    offset = 0,
                    publishedAtLt = oldestTime
                )
            } else {
                articleRepository.getArticles(
                    limit = params.loadSize,
                    offset = offset
                )
            }

            val nextKey = if (articles.isEmpty() || articles.size < params.loadSize) {
                null
            } else {
                offset + articles.size
            }
            LoadResult.Page(
                data = articles,
                prevKey = if (offset == 0) null else maxOf(0, offset - params.loadSize),
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
