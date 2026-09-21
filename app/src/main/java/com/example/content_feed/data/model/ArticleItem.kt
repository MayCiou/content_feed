package com.example.content_feed.data.model

data class ArticleItem(
    val id: Int,
    val imageUrl: String,
    val title: String,
    val publishedDate: String,
    val url: String = "",
    val isSaved: Boolean = false
)
