package com.example.content_feed.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import com.example.content_feed.data.local.WeatherDao
import com.example.content_feed.data.local.WeatherEntity
import com.example.content_feed.data.remote.OpenMeteoApiService
import com.example.content_feed.ui.WeatherUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.roundToInt

@Singleton
class WeatherRepository @Inject constructor(
    private val apiService: OpenMeteoApiService,
    private val weatherDao: WeatherDao,
    @param:ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "WeatherRepository"
        private const val CACHE_VALID_DURATION_MS = 15 * 60 * 1000L // 30 minutes
        private const val MIN_DISTANCE_THRESHOLD_METERS = 1000f // 1 km
        private const val DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss"
    }

    suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherUiState {
        return withContext(Dispatchers.IO) {
            val cachedWeather = weatherDao.getLatestWeather()
            val currentTime = System.currentTimeMillis()

            if (cachedWeather != null) {
                val timeElapsed = currentTime - cachedWeather.lastFetchedTimestamp
                val distanceMeters = FloatArray(1)
                Location.distanceBetween(
                    latitude,
                    longitude,
                    cachedWeather.latitude,
                    cachedWeather.longitude,
                    distanceMeters
                )
                val movedDistance = distanceMeters[0]

                // 只有在「距離 ≥ 1 km」或「距離上次 API ≥ 30 min」時才呼叫 API
                val isDistanceExceeded = movedDistance >= MIN_DISTANCE_THRESHOLD_METERS
                val isTimeExceeded = timeElapsed >= CACHE_VALID_DURATION_MS

                if (!isDistanceExceeded && !isTimeExceeded) {
                    Log.d(TAG, "Using cached weather: timeElapsed=${timeElapsed / 1000}s, distance=${movedDistance}m")
                    return@withContext WeatherUiState.Success(
                        city = cachedWeather.city,
                        temperature = cachedWeather.temperature,
                        weatherInfo = cachedWeather.weatherInfo
                    )
                }
            }

            try {
                // Parallelize weather API and city geocoder lookup
                val weatherDeferred = async {
                    apiService.getForecast(
                        latitude = latitude,
                        longitude = longitude
                    )
                }
                val cityDeferred = async {
                    withTimeoutOrNull(1200L) {
                        resolveCityName(latitude, longitude)
                    }
                }

                val response = weatherDeferred.await()
                val current = response.current ?: return@withContext WeatherUiState.LocationUnavailable
                val tempInt = current.temperature2m.roundToInt()
                val temperatureText = "$tempInt°"

                val weatherDesc = getWeatherDescription(current.weatherCode)

                val maxTemp = response.daily?.temperature2mMax?.firstOrNull()?.roundToInt()
                val minTemp = response.daily?.temperature2mMin?.firstOrNull()?.roundToInt()

                val weatherInfoText = if (maxTemp != null && minTemp != null) {
                    "$weatherDesc · H:$maxTemp° L:$minTemp°"
                } else {
                    weatherDesc
                }

                val cityName = cityDeferred.await() ?: "Unknown"

                val dateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.getDefault())
                val formattedTime = dateFormat.format(Date(currentTime))

                val weatherEntity = WeatherEntity(
                    id = 1,
                    city = cityName,
                    temperature = temperatureText,
                    weatherInfo = weatherInfoText,
                    lastFetchedTime = formattedTime,
                    lastFetchedTimestamp = currentTime,
                    latitude = latitude,
                    longitude = longitude
                )
                weatherDao.insertWeather(weatherEntity)
                Log.d(TAG, "Saved weather data with location ($latitude, $longitude) to Room at $formattedTime")

                WeatherUiState.Success(
                    city = cityName,
                    temperature = temperatureText,
                    weatherInfo = weatherInfoText
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching weather data: ${e.message}", e)
                WeatherUiState.LocationUnavailable
            }
        }
    }

    private suspend fun resolveCityName(latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null

        val geocoder = Geocoder(context, Locale.getDefault())

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val address = addresses.firstOrNull()
                            val city = address?.locality ?: address?.subAdminArea ?: address?.adminArea
                            if (continuation.isActive) {
                                continuation.resume(city)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    }
                )
            }
        } else {
            try {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()
                address?.locality ?: address?.subAdminArea ?: address?.adminArea
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear sky"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing Drizzle"
            61, 63, 65 -> "Rain"
            66, 67 -> "Freezing Rain"
            71, 73, 75 -> "Snow fall"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Partly cloudy"
        }
    }
}
