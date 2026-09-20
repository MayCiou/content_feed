package com.example.content_feed.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.content_feed.data.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _weatherUiState = MutableLiveData<WeatherUiState>()
    val weatherUiState: LiveData<WeatherUiState> = _weatherUiState

    fun onPermissionDenied() {
        _weatherUiState.value = WeatherUiState.PermissionDenied
    }

    fun fetchDataWithLocation() {
        viewModelScope.launch {
            val location = locationRepository.getCurrentLocation()
            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude
                Log.d("ReadingViewModel", "Location acquired: Lat=$lat, Lng=$lng")
                // Simulated weather data for demonstration
                _weatherUiState.value = WeatherUiState.Success(
                    city = "Taipei",
                    temperature = "34°",
                    weatherInfo = "Partly cloudy · H:35° L:28°"
                )
            } else {
                Log.d("ReadingViewModel", "Unable to get location.")
                _weatherUiState.value = WeatherUiState.LocationUnavailable
            }
        }
    }
}
