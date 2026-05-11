package zhaoyun.example.composedemo.story.background.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class BackgroundState(
    val backgroundImageUrl: String = "",
) : UiState
