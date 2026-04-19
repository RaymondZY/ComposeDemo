package zhaoyun.example.composedemo.domain.usecase

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import zhaoyun.example.composedemo.domain.model.TodoEffect
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.model.TodoItem
import zhaoyun.example.composedemo.domain.model.TodoState

/**
 * Todo 核心用例 —— 纯 Kotlin，不依赖任何平台框架
 *
 * 包含完整的业务状态机：输入验证、增删改查、清除已完成、副作用通知。
 * 依赖通过 Koin [inject] 字段注入。
 */
class TodoUseCases : KoinComponent {

    private val checkLoginUseCase: CheckLoginUseCase by inject()

    private val _state = MutableStateFlow(TodoState())
    val state: StateFlow<TodoState> = _state.asStateFlow()

    private val _effect = Channel<TodoEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var nextId = 1L

    fun onEvent(event: TodoEvent) {
        when (event) {
            is TodoEvent.OnInputTextChanged -> {
                _state.update { currentState ->
                    currentState.copy(
                        inputText = event.text,
                        isInputValid = event.text.isNotBlank()
                    )
                }
            }

            is TodoEvent.OnAddTodoClicked -> {
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
                    sendEffect(TodoEffect.ShowToast("添加成功"))
                }
            }

            is TodoEvent.OnTodoCheckedChanged -> {
                _state.update { state ->
                    state.copy(
                        todos = state.todos.map { todo ->
                            if (todo.id == event.id) {
                                todo.copy(isCompleted = event.isChecked)
                            } else {
                                todo
                            }
                        }
                    )
                }
            }

            is TodoEvent.OnTodoDeleteClicked -> {
                _state.update { state ->
                    state.copy(
                        todos = state.todos.filter { it.id != event.id }
                    )
                }
                sendEffect(TodoEffect.ShowToast("已删除"))
            }

            is TodoEvent.CheckLogin -> {
                val loggedIn = checkLoginUseCase()
                _state.update { it.copy(isLoggedIn = loggedIn) }
            }

            is TodoEvent.OnClearCompletedClicked -> {
                val completedCount = _state.value.todos.count { it.isCompleted }
                if (completedCount > 0) {
                    _state.update { state ->
                        state.copy(
                            todos = state.todos.filter { !it.isCompleted }
                        )
                    }
                    sendEffect(TodoEffect.ShowToast("已清除 $completedCount 个已完成任务"))
                }
            }
        }
    }

    private fun sendEffect(effect: TodoEffect) {
        _effect.trySend(effect)
    }
}
