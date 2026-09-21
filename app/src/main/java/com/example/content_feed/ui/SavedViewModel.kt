package com.example.content_feed.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.content_feed.data.model.ArticleItem
import com.example.content_feed.data.repository.ArticleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val savedSharedEvents: SavedSharedEvents
) : ViewModel() {

    val savedArticles: StateFlow<List<ArticleItem>> =
        articleRepository.getSavedArticlesStream()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun unsaveArticle(article: ArticleItem) {
        viewModelScope.launch {
            articleRepository.removeArticle(article.id)
            savedSharedEvents.notifyArticleRemoved(article.id)
        }
    }
}
