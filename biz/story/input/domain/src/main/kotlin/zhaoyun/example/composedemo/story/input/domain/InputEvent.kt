package zhaoyun.example.composedemo.story.input.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class InputEvent : UiEvent {
    data class OnTextChanged(val text: String) : InputEvent()
    data class OnFocusChanged(val focused: Boolean) : InputEvent()
    object OnBracketClicked : InputEvent()
    object OnVoiceClicked : InputEvent()
    object OnPlusClicked : InputEvent()
    object OnSendClicked : InputEvent()
}
