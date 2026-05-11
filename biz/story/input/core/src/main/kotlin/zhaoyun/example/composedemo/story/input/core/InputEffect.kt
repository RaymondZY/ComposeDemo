package zhaoyun.example.composedemo.story.input.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect

sealed class InputEffect : UiEffect {
    data class InsertBrackets(val newText: String, val cursorPosition: Int) : InputEffect()
    object ClearFocus : InputEffect()
}
