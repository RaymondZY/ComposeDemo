package zhaoyun.example.composedemo.feed.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.service.feed.api.model.FeedCard

data class FeedState(
    val cards: List<FeedCard> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 0,
    val hasMore: Boolean = true
) : UiState
