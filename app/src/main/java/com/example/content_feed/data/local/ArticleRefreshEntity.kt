package com.example.content_feed.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "article_refresh_meta")
data class ArticleRefreshEntity(
    @PrimaryKey
    val id: Int = 1,
    val lastArticleRefreshTime: String
)
