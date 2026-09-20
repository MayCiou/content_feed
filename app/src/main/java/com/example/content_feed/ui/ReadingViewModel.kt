package com.example.content_feed.ui

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.content_feed.data.model.ArticleItem
import com.example.content_feed.data.repository.ArticleRepository
import com.example.content_feed.data.repository.LocationRepository
import com.example.content_feed.data.repository.WeatherRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val weatherRepository: WeatherRepository,
    private val articleRepository: ArticleRepository
) : ViewModel() {

    sealed class ArticleUiState {
        object Initial : ArticleUiState()
        data class LocalCache(val articles: List<ArticleItem>) : ArticleUiState()
        object PagingApi : ArticleUiState()
    }

    private val _articleUiState = MutableStateFlow<ArticleUiState>(ArticleUiState.Initial)
    val articleUiState: StateFlow<ArticleUiState> = _articleUiState.asStateFlow()

    val articlesPagingData: Flow<PagingData<ArticleItem>> =
        articleRepository.getArticlesStream().cachedIn(viewModelScope)

    private val _weatherUiState = MutableLiveData<WeatherUiState>()
    val weatherUiState: LiveData<WeatherUiState> = _weatherUiState

    fun loadInitialArticles() {
        viewModelScope.launch {
            val shouldFetch = articleRepository.shouldFetchFromApi()
            if (shouldFetch) {
                Log.d("ReadingViewModel", "Refresh time > 1 hr or no cache, fetching from API via Paging.")
                _articleUiState.value = ArticleUiState.PagingApi
            } else {
                Log.d("ReadingViewModel", "Refresh time < 1 hr, loading cached articles ORDER BY publishedAt ASC.")
                val cached = articleRepository.getCachedArticlesAsc()
                _articleUiState.value = ArticleUiState.LocalCache(cached)
            }
        }
    }

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
