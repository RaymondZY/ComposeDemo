package zhaoyun.example.composedemo.story.storypanel.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect

sealed class StoryPanelEffect : UiEffect {
    data object NavigateBack : StoryPanelEffect()
}
