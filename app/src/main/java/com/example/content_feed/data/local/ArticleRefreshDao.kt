package com.example.content_feed.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArticleRefreshDao {

    @Query("SELECT lastArticleRefreshTime FROM article_refresh_meta WHERE id = 1 LIMIT 1")
    suspend fun getLastArticleRefreshTime(): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateArticleRefresh(entity: ArticleRefreshEntity)
}
