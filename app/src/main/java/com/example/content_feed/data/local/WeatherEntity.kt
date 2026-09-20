package com.example.content_feed.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather")
data class WeatherEntity(
    @PrimaryKey
    val id: Int = 1,
    val city: String,
    val temperature: String,
    val weatherInfo: String,
    val lastFetchedTime: String,
    val lastFetchedTimestamp: Long
)
