package zhaoyun.example.composedemo.login.domain.usecase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import zhaoyun.example.composedemo.service.usercenter.api.UserRepository
import zhaoyun.example.composedemo.service.usercenter.api.model.LoginResult

/**
 * 登录用例 —— 演示 :biz 通过服务发现（Koin）获取 :service API 实例
 */
class LoginUseCase : KoinComponent {

    private val userRepository: UserRepository by inject()

    suspend operator fun invoke(username: String, password: String): LoginResult {
        return userRepository.login(username, password)
    }
}
