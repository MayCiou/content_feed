package com.example.content_feed.data.remote.model

import com.google.gson.annotations.SerializedName

data class ArticleListResponse(
    @SerializedName("count")
    val count: Int,
    @SerializedName("next")
    val next: String?,
    @SerializedName("previous")
    val previous: String?,
    @SerializedName("results")
    val results: List<ArticleDto>
)

data class ArticleDto(
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("image_url")
    val imageUrl: String?,
    @SerializedName("published_at")
    val publishedAt: String?,
    val isSaved: Boolean = false
)
