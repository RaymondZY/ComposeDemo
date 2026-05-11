package zhaoyun.example.composedemo.story.infobar.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class InfoBarState(
    val storyTitle: String = "",
    val creatorName: String = "",
    val creatorHandle: String = "",
    val likes: Int = 0,
    val shares: Int = 0,
    val comments: Int = 0,
    val isLiked: Boolean = false,
) : UiState
