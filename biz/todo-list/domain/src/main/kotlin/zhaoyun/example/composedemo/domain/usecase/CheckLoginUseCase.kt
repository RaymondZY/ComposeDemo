package zhaoyun.example.composedemo.domain.usecase

import zhaoyun.example.composedemo.domain.model.TodoEffect
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.model.TodoState
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase
import zhaoyun.example.composedemo.service.usercenter.api.UserRepository

/**
 * 检查登录状态用例 —— 作为独立的 MVI UseCase 运行
 *
 * 消费 [TodoEvent.CheckLogin] 并更新 [TodoState.isLoggedIn]。
 * 与 [TodoUseCases] 共享同一份 State，由 [TodoViewModel] 统一绑定。
 */
class CheckLoginUseCase(
    private val userRepository: UserRepository
) : BaseUseCase<TodoState, TodoEvent, TodoEffect>(TodoState()) {

    override suspend fun onEvent(event: TodoEvent) {
        when (event) {
            is TodoEvent.CheckLogin -> {
                val loggedIn = userRepository.isLoggedIn()
                updateState { it.copy(isLoggedIn = loggedIn) }
            }
            else -> { /* 忽略不相关的事件 */ }
        }
    }
}
