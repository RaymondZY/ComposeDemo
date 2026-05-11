package zhaoyun.example.composedemo.story.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class StoryCardEvent : UiEvent {
    data object OnRefresh : StoryCardEvent()
    data object OnLoadMore : StoryCardEvent()

    sealed class Message : StoryCardEvent() {
        data object OnDialogueClicked : Message()
    }

    sealed class InfoBar : StoryCardEvent() {
        data object OnLikeClicked : InfoBar()
        data object OnShareClicked : InfoBar()
        data object OnCommentClicked : InfoBar()
        data object OnHistoryClicked : InfoBar()
    }

    sealed class Input : StoryCardEvent() {
        data object OnFocused : Input()
        data object OnInputClicked : Input()
        data object OnSendClicked : Input()
    }
}
