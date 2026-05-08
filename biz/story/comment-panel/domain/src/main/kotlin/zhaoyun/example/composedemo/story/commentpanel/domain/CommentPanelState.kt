package zhaoyun.example.composedemo.story.commentpanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class CommentPanelState(
    val cardId: String = "",
    val comments: List<CommentItem> = emptyList(),
    val inputText: String = "",
    val isLoadingComments: Boolean = false,
    val isSendingComment: Boolean = false,
) : UiState
