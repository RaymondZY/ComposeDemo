package zhaoyun.example.composedemo.todo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.usecase.TodoUseCases

/**
 * Todo ViewModel —— 表现层仅负责生命周期管理与平台桥接
 *
 * 所有业务逻辑已下沉到 [TodoUseCases]（位于 :domain 模块），
 * 该 ViewModel **仅**将 UI 事件转发给 UseCase，并暴露状态流供 Compose 订阅。
 * 依赖通过构造函数注入。
 */
class TodoViewModel(
    private val todoUseCases: TodoUseCases
) : ViewModel() {

    val state = todoUseCases.state
    val effect = todoUseCases.effect

    fun onEvent(event: TodoEvent) {
        viewModelScope.launch {
            todoUseCases.onEvent(event)
        }
    }
}
