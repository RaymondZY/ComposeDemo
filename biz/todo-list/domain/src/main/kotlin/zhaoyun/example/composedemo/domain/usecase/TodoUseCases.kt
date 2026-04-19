package zhaoyun.example.composedemo.domain.usecase

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import zhaoyun.example.composedemo.core.mvi.UiEffect
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.model.TodoItem
import zhaoyun.example.composedemo.domain.model.TodoState

/**
 * Todo 核心用例 —— 纯 Kotlin，不依赖任何平台框架
 *
 * 包含完整的业务状态机：输入验证、增删改查、清除已完成、副作用通知。
 * 依赖通过构造函数传入。
 */
class TodoUseCases(
    private val checkLoginUseCase: CheckLoginUseCase
) {

    private val _state = MutableStateFlow(TodoState())
    val state: StateFlow<TodoState> = _state.asStateFlow()

    private val _effect = Channel<UiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var nextId = 1L

    fun onEvent(event: TodoEvent) {
        when (event) {
            is TodoEvent.OnInputTextChanged -> handleInputTextChanged(event.text)
            is TodoEvent.OnAddTodoClicked -> handleAddTodo()
            is TodoEvent.OnTodoCheckedChanged -> handleTodoCheckedChanged(event.id, event.isChecked)
            is TodoEvent.OnTodoDeleteClicked -> handleTodoDeleteClicked(event.id)
            is TodoEvent.CheckLogin -> handleCheckLogin()
            is TodoEvent.OnClearCompletedClicked -> handleClearCompleted()
        }
    }

    private fun handleInputTextChanged(text: String) {
        _state.update { currentState ->
            currentState.copy(
                inputText = text,
                isInputValid = text.isNotBlank()
            )
        }
    }

    private fun handleAddTodo() {
        val currentState = _state.value
        if (currentState.isInputValid) {
            val newTodo = TodoItem(
                id = nextId++,
                title = currentState.inputText.trim()
            )
            _state.update { state ->
                state.copy(
                    todos = state.todos + newTodo,
                    inputText = "",
                    isInputValid = false
                )
            }
            sendEffect(UiEffect.ShowToast("添加成功"))
        }
    }

    private fun handleTodoCheckedChanged(id: Long, isChecked: Boolean) {
        _state.update { state ->
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
        _state.update { state ->
            state.copy(
                todos = state.todos.filter { it.id != id }
            )
        }
        sendEffect(UiEffect.ShowToast("已删除"))
    }

    private fun handleCheckLogin() {
        val loggedIn = checkLoginUseCase()
        _state.update { it.copy(isLoggedIn = loggedIn) }
    }

    private fun handleClearCompleted() {
        val completedCount = _state.value.todos.count { it.isCompleted }
        if (completedCount > 0) {
            _state.update { state ->
                state.copy(
                    todos = state.todos.filter { !it.isCompleted }
                )
            }
            sendEffect(UiEffect.ShowToast("已清除 $completedCount 个已完成任务"))
        }
    }

    private fun sendEffect(effect: UiEffect) {
        _effect.trySend(effect)
    }
}
