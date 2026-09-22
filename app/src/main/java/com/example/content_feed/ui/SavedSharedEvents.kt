package com.example.content_feed.ui

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavedSharedEvents @Inject constructor() {
    private val _removedArticleIds = MutableSharedFlow<Int>(extraBufferCapacity = 64)
    val removedArticleIds: SharedFlow<Int> = _removedArticleIds.asSharedFlow()

    fun notifyArticleRemoved(articleId: Int) {
        _removedArticleIds.tryEmit(articleId)
    }
}
