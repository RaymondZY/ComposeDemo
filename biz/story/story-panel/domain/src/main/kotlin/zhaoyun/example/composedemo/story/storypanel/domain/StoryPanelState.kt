package zhaoyun.example.composedemo.story.storypanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class StoryPanelState(
    val cardId: String = "",
) : UiState
