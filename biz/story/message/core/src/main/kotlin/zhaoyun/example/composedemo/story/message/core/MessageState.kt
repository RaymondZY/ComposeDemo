package zhaoyun.example.composedemo.story.message.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class MessageState(
    val characterName: String = "",
    val characterSubtitle: String = "",
    val dialogueText: String = "",
    val isExpanded: Boolean = false,
) : UiState
