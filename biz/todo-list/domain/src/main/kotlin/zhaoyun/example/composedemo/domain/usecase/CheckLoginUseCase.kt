package zhaoyun.example.composedemo.domain.usecase

import zhaoyun.example.composedemo.service.usercenter.api.UserRepository
import zhaoyun.example.composedemo.service.usercenter.api.model.UserInfo

/**
 * 检查登录状态用例 —— 纯 Kotlin，通过构造函数接收依赖
 */
class CheckLoginUseCase(
    private val userRepository: UserRepository
) {

    /**
     * 是否已登录
     */
    operator fun invoke(): Boolean = userRepository.isLoggedIn()

    /**
     * 获取当前登录用户信息，未登录时返回 null
     */
    fun getCurrentUser(): UserInfo? = userRepository.currentUser.value
}
