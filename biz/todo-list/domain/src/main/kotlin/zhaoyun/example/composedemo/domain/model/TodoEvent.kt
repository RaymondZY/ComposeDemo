package zhaoyun.example.composedemo.domain.model

sealed class TodoEvent {
    data class OnInputTextChanged(val text: String) : TodoEvent()
    data object OnAddTodoClicked : TodoEvent()
    data class OnTodoCheckedChanged(val id: Long, val isChecked: Boolean) : TodoEvent()
    data class OnTodoDeleteClicked(val id: Long) : TodoEvent()
    data object OnClearCompletedClicked : TodoEvent()
    data object CheckLogin : TodoEvent()
}
