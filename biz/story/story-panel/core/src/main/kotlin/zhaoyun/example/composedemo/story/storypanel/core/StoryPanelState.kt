package zhaoyun.example.composedemo.story.storypanel.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class StoryPanelState(
    val cardId: String = "",
) : UiState
