package com.example.content_feed.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.example.content_feed.data.remote.OpenMeteoApiService
import com.example.content_feed.ui.WeatherUiState
import com.example.content_feed.util.NetworkUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.math.roundToInt

@Singleton
class WeatherRepository @Inject constructor(
    private val apiService: OpenMeteoApiService,
    private val networkUtil: NetworkUtil,
    @param:ApplicationContext private val context: Context
) {

    suspend fun getWeatherData(latitude: Double, longitude: Double): WeatherUiState {
        if (!networkUtil.isNetworkAvailable()) {
            return WeatherUiState.LocationUnavailable
        }

        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getForecast(
                    latitude = latitude,
                    longitude = longitude
                )

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

                val cityName = resolveCityName(latitude, longitude) ?: "Taipei"

                WeatherUiState.Success(
                    city = cityName,
                    temperature = temperatureText,
                    weatherInfo = weatherInfoText
                )
            } catch (e: Exception) {
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
