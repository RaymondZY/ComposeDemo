package zhaoyun.example.composedemo.service.usercenter.impl

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import zhaoyun.example.composedemo.service.usercenter.api.UserRepository
import zhaoyun.example.composedemo.service.usercenter.api.model.LoginResult
import zhaoyun.example.composedemo.service.usercenter.api.model.UserInfo

/**
 * Mock 实现 —— 模拟服务端登录逻辑与本地持久化
 *
 * 用户名: admin  密码: 123456  为固定合法账号
 */
class MockUserRepository : UserRepository {

    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    override val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()

    override fun isLoggedIn(): Boolean = _currentUser.value != null

    override suspend fun login(username: String, password: String): LoginResult {
        // 模拟网络延迟
        delay(800)

        return if (username == "admin" && password == "123456") {
            val user = UserInfo(
                userId = "u_10086",
                username = "admin",
                displayName = "管理员"
            )
            _currentUser.value = user
            LoginResult.Success(user)
        } else {
            LoginResult.Failure("用户名或密码错误")
        }
    }

    override fun logout() {
        _currentUser.value = null
    }
}
