package zhaoyun.example.composedemo.story.message.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class MessageEvent : UiEvent {
    object OnDialogueClicked : MessageEvent()
}
