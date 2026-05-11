package zhaoyun.example.composedemo.feed.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class FeedState(
    val errorMessage: String? = null,
) : UiState
