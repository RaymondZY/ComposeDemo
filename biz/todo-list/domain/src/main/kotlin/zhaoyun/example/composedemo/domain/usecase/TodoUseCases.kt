package zhaoyun.example.composedemo.domain.usecase

import zhaoyun.example.composedemo.domain.model.TodoEffect
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.model.TodoItem
import zhaoyun.example.composedemo.domain.model.TodoState
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase

/**
 * Todo 核心用例 —— 纯 Kotlin，不依赖任何平台框架
 *
 * 包含完整的业务状态机：输入验证、增删改查、清除已完成、副作用通知。
 */
class TodoUseCases : BaseUseCase<TodoState, TodoEvent, TodoEffect>(TodoState()) {

    private var nextId = 1L

    override suspend fun onEvent(event: TodoEvent) {
        when (event) {
            is TodoEvent.OnInputTextChanged -> handleInputTextChanged(event.text)
            is TodoEvent.OnAddTodoClicked -> handleAddTodo()
            is TodoEvent.OnTodoCheckedChanged -> handleTodoCheckedChanged(event.id, event.isChecked)
            is TodoEvent.OnTodoDeleteClicked -> handleTodoDeleteClicked(event.id)
            is TodoEvent.OnClearCompletedClicked -> handleClearCompleted()
            else -> { /* 忽略其他事件（如 CheckLogin 由 CheckLoginUseCase 处理） */ }
        }
    }

    private fun handleInputTextChanged(text: String) {
        updateState { currentState ->
            currentState.copy(
                inputText = text,
                isInputValid = text.isNotBlank()
            )
        }
    }

    private fun handleAddTodo() {
        if (currentState.isInputValid) {
            val newTodo = TodoItem(
                id = nextId++,
                title = currentState.inputText.trim()
            )
            updateState { state ->
                state.copy(
                    todos = state.todos + newTodo,
                    inputText = "",
                    isInputValid = false
                )
            }
            sendBaseEffect(BaseEffect.ShowToast("添加成功"))
        }
    }

    private fun handleTodoCheckedChanged(id: Long, isChecked: Boolean) {
        updateState { state ->
            state.copy(
                todos = state.todos.map { todo ->
                    if (todo.id == id) {
                        todo.copy(isCompleted = isChecked)
                    } else {
                        todo
                    }
                }
            )
        }
    }

    private fun handleTodoDeleteClicked(id: Long) {
        updateState { state ->
            state.copy(
                todos = state.todos.filter { it.id != id }
            )
        }
        sendBaseEffect(BaseEffect.ShowToast("已删除"))
    }

    private fun handleClearCompleted() {
        val completedCount = currentState.todos.count { it.isCompleted }
        if (completedCount > 0) {
            updateState { state ->
                state.copy(
                    todos = state.todos.filter { !it.isCompleted }
                )
            }
            sendBaseEffect(BaseEffect.ShowToast("已清除 $completedCount 个已完成任务"))
        }
    }
}
