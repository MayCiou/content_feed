package com.example.content_feed.data.remote

import com.example.content_feed.data.remote.model.ArticleListResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface SpaceflightApiService {

    @GET("v4/articles/")
    suspend fun getArticles(
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0,
        @Query("published_at_lt") publishedAtLt: String? = null
    ): ArticleListResponse
}
