package zhaoyun.example.composedemo.todo.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.usecase.TodoUseCases

/**
 * Todo ViewModel —— 表现层仅负责生命周期管理与平台桥接
 *
 * 所有业务逻辑已下沉到 [TodoUseCases]（位于 :domain 模块），
 * 该 ViewModel **仅**将 UI 事件转发给 UseCase，并暴露状态流供 Compose 订阅。
 * 与 UI 的所有通信均通过 [TodoEvent] 单向完成。
 * 依赖通过 Koin [inject] 字段注入。
 */
class TodoViewModel : ViewModel(), KoinComponent {

    private val todoUseCases: TodoUseCases by inject()

    val state = todoUseCases.state
    val effect = todoUseCases.effect

    fun onEvent(event: TodoEvent) {
        viewModelScope.launch {
            todoUseCases.onEvent(event)
        }
    }
}
