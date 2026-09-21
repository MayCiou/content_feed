package com.example.content_feed.data.remote.model

import com.google.gson.annotations.SerializedName

data class OpenMeteoResponse(
    @SerializedName("latitude")
    val latitude: Double,
    @SerializedName("longitude")
    val longitude: Double,
    @SerializedName("current")
    val current: CurrentWeather?,
    @SerializedName("daily")
    val daily: DailyForecast?
)

data class CurrentWeather(
    @SerializedName("time")
    val time: String,
    @SerializedName("temperature_2m")
    val temperature2m: Double,
    @SerializedName("weather_code")
    val weatherCode: Int
)

data class DailyForecast(
    @SerializedName("time")
    val time: List<String>,
    @SerializedName("temperature_2m_max")
    val temperature2mMax: List<Double>,
    @SerializedName("temperature_2m_min")
    val temperature2mMin: List<Double>
)
