package zhaoyun.example.composedemo.story.storypanel.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class StoryPanelEvent : UiEvent {
    data object OnDismiss : StoryPanelEvent()
}
