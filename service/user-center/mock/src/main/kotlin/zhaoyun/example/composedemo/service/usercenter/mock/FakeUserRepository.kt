package zhaoyun.example.composedemo.service.usercenter.mock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import zhaoyun.example.composedemo.service.usercenter.api.UserRepository
import zhaoyun.example.composedemo.service.usercenter.api.model.LoginResult
import zhaoyun.example.composedemo.service.usercenter.api.model.UserInfo

/**
 * 用户中心内存假实现 —— 供各 :domain / :presentation 模块的单元测试共用
 *
 * 默认合法账号: admin / 123456
 */
class FakeUserRepository : UserRepository {

    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    override val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()

    override fun isLoggedIn(): Boolean = _currentUser.value != null

    override suspend fun login(username: String, password: String): LoginResult {
        return if (username == "admin" && password == "123456") {
            val user = UserInfo("u_10086", username, "管理员")
            _currentUser.value = user
            LoginResult.Success(user)
        } else {
            LoginResult.Failure("用户名或密码错误")
        }
    }

    /**
     * 直接设置登录用户（测试辅助方法）
     */
    fun setLoggedInUser(user: UserInfo) {
        _currentUser.value = user
    }

    override fun logout() {
        _currentUser.value = null
    }
}
