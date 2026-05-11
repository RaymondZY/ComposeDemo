package zhaoyun.example.composedemo.story.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect

sealed class StoryCardEffect : UiEffect {
    sealed class InfoBar : StoryCardEffect() {
        data class OpenSharePanel(val cardId: String) : InfoBar()
        data class OpenCommentPanel(val cardId: String) : InfoBar()
        data class ShowHistory(val cardId: String) : InfoBar()
    }

    sealed class Input : StoryCardEffect() {
        data class NavigateToChat(val cardId: String) : Input()
        data class SendMessage(val cardId: String, val text: String) : Input()
    }
}
