package zhaoyun.example.composedemo.feed.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class FeedState(
    val errorMessage: String? = null,
) : UiState
