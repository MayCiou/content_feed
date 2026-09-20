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
        _apiLoadingStatus.value = "定位中..."
        viewModelScope.launch {
            val location = locationRepository.getCurrentLocation()
            if (location != null) {
                val lat = location.latitude
                val lng = location.longitude
                // 模擬在此處使用經緯度呼叫 API，數值完全不暴露給 View 層
                Log.d("ReadingViewModel", "成功取得經緯度，內部呼叫 API: 緯度=$lat, 經度=$lng")
                _apiLoadingStatus.value = "成功取得位置，API 呼叫完成"
            } else {
                Log.d("ReadingViewModel", "無法取得定位，使用預設位置呼叫 API")
                _apiLoadingStatus.value = "定位失敗，已使用備用方案"
            }
        }
    }
}
