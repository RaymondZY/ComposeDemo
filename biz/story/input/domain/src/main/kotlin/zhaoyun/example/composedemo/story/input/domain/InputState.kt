package zhaoyun.example.composedemo.story.input.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class InputState(
    val hintText: String = "自由输入...",
    val isFocused: Boolean = false,
) : UiState
