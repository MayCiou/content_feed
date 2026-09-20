package com.example.content_feed.ui

sealed class WeatherUiState {
    object Loading : WeatherUiState()

    data class Success(
        val city: String,
        val temperature: String,
        val weatherInfo: String
    ) : WeatherUiState()

    object PermissionDenied : WeatherUiState()
    object LocationUnavailable : WeatherUiState()
    object NetworkUnavailable : WeatherUiState()
}
