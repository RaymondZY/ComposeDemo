package zhaoyun.example.composedemo.story.commentpanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class CommentPanelState(
    val cardId: String = "",
) : UiState
