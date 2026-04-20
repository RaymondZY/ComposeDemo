package zhaoyun.example.composedemo.domain.model

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class TodoState(
    val todos: List<TodoItem> = emptyList(),
    val inputText: String = "",
    val isInputValid: Boolean = false,
    val isLoggedIn: Boolean? = null
) : UiState
