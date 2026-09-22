package com.example.content_feed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.content_feed.data.model.ArticleItem
import com.example.content_feed.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val savedSharedEvents: SavedSharedEvents
) : ViewModel() {

    val uiState: StateFlow<SavedUiState> =
        articleRepository.getSavedArticlesStream()
            .map<List<ArticleItem>, SavedUiState> { SavedUiState.Success(it) }
            .catch { emit(SavedUiState.Error(it.localizedMessage ?: "Failed to load saved articles")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SavedUiState.Loading
            )

    fun unsaveArticle(article: ArticleItem) {
        viewModelScope.launch {
            articleRepository.removeArticle(article.id)
            savedSharedEvents.notifyArticleRemoved(article.id)
        }
    }
}
