package zhaoyun.example.composedemo.domain.model

data class TodoState(
    val todos: List<TodoItem> = emptyList(),
    val inputText: String = "",
    val isInputValid: Boolean = false,
    val isLoggedIn: Boolean? = null
)
