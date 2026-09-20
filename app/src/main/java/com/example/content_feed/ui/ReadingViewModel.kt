package com.example.content_feed.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.content_feed.data.repository.LocationRepository
import com.example.content_feed.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _weatherUiState = MutableLiveData<WeatherUiState>()
    val weatherUiState: LiveData<WeatherUiState> = _weatherUiState

    fun onPermissionDenied() {
        _weatherUiState.value = WeatherUiState.PermissionDenied
    }

    fun fetchDataWithLocation() {
        _weatherUiState.value = WeatherUiState.Loading
        viewModelScope.launch {
            val location = locationRepository.getCurrentLocation()
            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude
                Log.d("ReadingViewModel", "Location acquired: Lat=$lat, Lng=$lng")
                val state = weatherRepository.getWeatherData(lat, lng)
                _weatherUiState.value = state
            } else {
                Log.d("ReadingViewModel", "Unable to get location.")
                _weatherUiState.value = WeatherUiState.LocationUnavailable
            }
        }
    }
}
