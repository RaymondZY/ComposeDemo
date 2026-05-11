package zhaoyun.example.composedemo.story.storypanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class StoryPanelEvent : UiEvent {
    data object OnDismiss : StoryPanelEvent()
}
