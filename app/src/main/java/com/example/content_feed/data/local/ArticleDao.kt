package com.example.content_feed.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles ORDER BY publishedAt ASC")
    suspend fun getAllArticlesAsc(): List<ArticleEntity>

    @Query("SELECT MIN(publishedAt) FROM articles")
    suspend fun getOldestPublishedAt(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("DELETE FROM articles")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun getArticleCount(): Int
}
