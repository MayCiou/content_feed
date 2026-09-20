package com.example.content_feed.di

import android.content.Context
import androidx.room.Room
import com.example.content_feed.data.local.AppDatabase
import com.example.content_feed.data.local.ArticleRefreshDao
import com.example.content_feed.data.local.WeatherDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "content_feed_database"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    @Provides
    fun provideWeatherDao(appDatabase: AppDatabase): WeatherDao {
        return appDatabase.weatherDao()
    }

    @Provides
    fun provideArticleRefreshDao(appDatabase: AppDatabase): ArticleRefreshDao {
        return appDatabase.articleRefreshDao()
    }
}
