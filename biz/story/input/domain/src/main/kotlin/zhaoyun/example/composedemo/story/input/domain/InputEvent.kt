package zhaoyun.example.composedemo.story.input.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class InputEvent : UiEvent {
    object OnFocused : InputEvent()
    object OnInputClicked : InputEvent()
    object OnSendClicked : InputEvent()
}
