package zhaoyun.example.composedemo.story.commentpanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class CommentPanelEvent : UiEvent {
    data object OnPanelShown : CommentPanelEvent()
    data class OnInputChanged(val text: String) : CommentPanelEvent()
    data object OnSendClicked : CommentPanelEvent()
}
