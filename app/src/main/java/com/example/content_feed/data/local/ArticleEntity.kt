package com.example.content_feed.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "articles")
data class ArticleEntity(
    @PrimaryKey
    val id: Int,
    val imageUrl: String,
    val title: String,
    val publishedAt: String,
    val isSaved: Boolean = false
)
