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

    private val _apiLoadingStatus = MutableLiveData<String>()
    val apiLoadingStatus: LiveData<String> = _apiLoadingStatus

    fun fetchDataWithLocation() {
        _apiLoadingStatus.value = "Locating..."
        viewModelScope.launch {
            val location = locationRepository.getCurrentLocation()
            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude
                // Simulate calling API with coordinates here, values are not exposed to the View layer
                Log.d("ReadingViewModel", "Location acquired, calling API internally: Lat=$lat, Lng=$lng")
                _apiLoadingStatus.value = "Location acquired, API call completed."
            } else {
                Log.d("ReadingViewModel", "Unable to get location, using default location for API call.")
                _apiLoadingStatus.value = "Location failed, fallback used."
            }
        }
    }
}
