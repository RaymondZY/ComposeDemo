package zhaoyun.example.composedemo.domain.usecase

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import zhaoyun.example.composedemo.service.usercenter.api.UserRepository
import zhaoyun.example.composedemo.service.usercenter.api.model.UserInfo

/**
 * 检查登录状态用例 —— 演示 :biz 通过服务发现框架（Koin）获取 :service API 实例
 *
 * biz 模块只依赖 :service:user-center:api，运行时由 :app 通过 Koin 将 api 绑定到 impl。
 */
class CheckLoginUseCase : KoinComponent {

    private val userRepository: UserRepository by inject()

    /**
     * 是否已登录
     */
    operator fun invoke(): Boolean = userRepository.isLoggedIn()

    /**
     * 获取当前登录用户信息，未登录时返回 null
     */
    fun getCurrentUser(): UserInfo? = userRepository.currentUser.value
}
