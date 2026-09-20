package com.example.content_feed.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WeatherEntity::class, ArticleRefreshEntity::class, ArticleEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun weatherDao(): WeatherDao
    abstract fun articleRefreshDao(): ArticleRefreshDao
    abstract fun articleDao(): ArticleDao
}
