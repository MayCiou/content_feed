package com.example.content_feed.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles ORDER BY publishedAt ASC")
    suspend fun getAllArticlesAsc(): List<ArticleEntity>

    @Query("SELECT MIN(publishedAt) FROM articles")
    suspend fun getOldestPublishedAt(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: ArticleEntity)

    @Query("UPDATE articles SET isSaved = :isSaved WHERE id = :articleId")
    suspend fun updateSavedStatus(articleId: Int, isSaved: Boolean)

    @Query("DELETE FROM articles WHERE id = :articleId")
    suspend fun deleteArticleById(articleId: Int)

    @Query("SELECT isSaved FROM articles WHERE id = :articleId LIMIT 1")
    suspend fun isArticleSaved(articleId: Int): Boolean?

    @Query("SELECT * FROM articles WHERE isSaved = 1 ORDER BY publishedAt DESC")
    suspend fun getSavedArticles(): List<ArticleEntity>

    @Query("SELECT * FROM articles WHERE isSaved = 1 ORDER BY publishedAt DESC")
    fun getSavedArticlesFlow(): Flow<List<ArticleEntity>>

    @Query("DELETE FROM articles")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun getArticleCount(): Int
}
