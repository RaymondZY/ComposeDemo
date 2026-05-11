package zhaoyun.example.composedemo.story.commentpanel.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class CommentPanelEvent : UiEvent {
    data object OnPanelShown : CommentPanelEvent()
    data object OnRetryInitialLoad : CommentPanelEvent()
    data object OnLoadMoreComments : CommentPanelEvent()
    data object OnDialogueEntryClicked : CommentPanelEvent()
    data class OnCommentExpanded(val commentId: String) : CommentPanelEvent()
    data class OnCommentLikeClicked(val commentId: String) : CommentPanelEvent()
    data class OnRepliesExpanded(val commentId: String) : CommentPanelEvent()
    data class OnRepliesCollapsed(val commentId: String) : CommentPanelEvent()
    data class OnLoadMoreReplies(val commentId: String) : CommentPanelEvent()
    data class OnInputChanged(val text: String) : CommentPanelEvent()
    data object OnSendClicked : CommentPanelEvent()
}
