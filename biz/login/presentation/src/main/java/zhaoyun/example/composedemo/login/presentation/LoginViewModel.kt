package zhaoyun.example.composedemo.login.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import zhaoyun.example.composedemo.login.domain.model.LoginEvent
import zhaoyun.example.composedemo.login.domain.usecase.LoginUseCase

/**
 * Login ViewModel —— 表现层仅负责生命周期管理与平台桥接
 *
 * 所有业务逻辑已下沉到 [LoginUseCase]（位于 :domain 模块），
 * 该 ViewModel **仅**将 UI 事件转发给 UseCase，并暴露状态流供 Compose 订阅。
 * 依赖通过构造函数注入。
 */
class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    val state = loginUseCase.state
    val effect = loginUseCase.effect
    val baseEffect = loginUseCase.baseEffect

    fun onEvent(event: LoginEvent) {
        viewModelScope.launch {
            loginUseCase.onEvent(event)
        }
    }
}
