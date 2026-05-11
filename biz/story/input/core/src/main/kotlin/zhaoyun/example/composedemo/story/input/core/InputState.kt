package zhaoyun.example.composedemo.story.input.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class InputState(
    val text: String = "",
    val isFocused: Boolean = false,
    val hintText: String = "自由输入...",
) : UiState
