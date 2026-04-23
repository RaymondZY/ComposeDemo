package zhaoyun.example.composedemo.login.domain.usecase

import zhaoyun.example.composedemo.login.domain.model.LoginEffect
import zhaoyun.example.composedemo.login.domain.model.LoginEvent
import zhaoyun.example.composedemo.login.domain.model.LoginState
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase
import zhaoyun.example.composedemo.service.storage.api.KeyValueStorage
import zhaoyun.example.composedemo.service.usercenter.api.UserRepository
import zhaoyun.example.composedemo.service.usercenter.api.model.LoginResult

/**
 * 登录用例 —— 纯 Kotlin，通过构造函数接收依赖
 *
 * 包含完整的 MVI 状态机：状态管理、事件分发、副作用发射。
 */
class LoginUseCase(
    private val userRepository: UserRepository,
    private val storage: KeyValueStorage
) : BaseUseCase<LoginState, LoginEvent, LoginEffect>(LoginState()) {

    override suspend fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.OnUsernameChanged -> updateState { it.copy(username = event.username, errorMessage = null) }
            is LoginEvent.OnPasswordChanged -> updateState { it.copy(password = event.password, errorMessage = null) }
            is LoginEvent.OnLoginClicked -> performLogin()
        }
    }

    private suspend fun performLogin() {
        val state = currentState
        if (state.username.isBlank() || state.password.isBlank()) {
            updateState { it.copy(errorMessage = "用户名和密码不能为空") }
            return
        }

        updateState { it.copy(isLoading = true, errorMessage = null) }
        when (val result = userRepository.login(state.username, state.password)) {
            is LoginResult.Success -> {
                storage.putString("last_username", state.username)
                updateState { it.copy(isLoading = false) }
                sendEffect(LoginEffect.NavigateToHome)
            }
            is LoginResult.Failure -> {
                updateState { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }
}
