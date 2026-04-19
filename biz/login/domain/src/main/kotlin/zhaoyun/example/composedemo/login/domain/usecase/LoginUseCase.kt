package zhaoyun.example.composedemo.login.domain.usecase

import zhaoyun.example.composedemo.service.usercenter.api.UserRepository
import zhaoyun.example.composedemo.service.usercenter.api.model.LoginResult

/**
 * 登录用例 —— 纯 Kotlin，通过构造函数接收依赖
 */
class LoginUseCase(
    private val userRepository: UserRepository
) {

    suspend operator fun invoke(username: String, password: String): LoginResult {
        return userRepository.login(username, password)
    }
}
