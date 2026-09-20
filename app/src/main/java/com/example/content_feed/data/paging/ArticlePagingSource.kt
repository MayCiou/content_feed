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

        private const val TAG = "ArticlePagingSource"
        const val PAGE_SIZE = 10
    }

    override fun getRefreshKey(state: PagingState<Int, ArticleItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(PAGE_SIZE)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(PAGE_SIZE)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ArticleItem> {
        val offset = params.key ?: 0
        return try {
            val articles = articleRepository.getArticles(limit = params.loadSize, offset = offset)
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
            Log.d(TAG, "error: $e")
            LoadResult.Error(e)
        }
    }
}
