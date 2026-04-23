package zhaoyun.example.composedemo.story.input.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect

sealed class InputEffect : UiEffect {
    data class NavigateToChat(val cardId: String) : InputEffect()
    data class SendMessage(val cardId: String, val text: String) : InputEffect()
}
