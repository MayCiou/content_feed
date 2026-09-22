package com.example.content_feed.ui

import com.example.content_feed.data.model.ArticleItem

sealed interface SavedUiState {
    object Loading : SavedUiState
    data class Success(val articles: List<ArticleItem>) : SavedUiState
    data class Error(val message: String) : SavedUiState
}
