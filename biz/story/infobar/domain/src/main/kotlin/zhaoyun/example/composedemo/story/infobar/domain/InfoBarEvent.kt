package zhaoyun.example.composedemo.story.infobar.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class InfoBarEvent : UiEvent {
    data object OnLikeClicked : InfoBarEvent()
    data object OnShareClicked : InfoBarEvent()
    data object OnCommentClicked : InfoBarEvent()
    data object OnHistoryClicked : InfoBarEvent()
}
