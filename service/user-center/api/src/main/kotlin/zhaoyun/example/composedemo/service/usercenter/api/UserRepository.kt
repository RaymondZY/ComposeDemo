package zhaoyun.example.composedemo.service.usercenter.api

import kotlinx.coroutines.flow.StateFlow
import zhaoyun.example.composedemo.service.usercenter.api.model.LoginResult
import zhaoyun.example.composedemo.service.usercenter.api.model.UserInfo

/**
 * 用户中心仓库接口 —— 纯 Kotlin，不依赖任何平台框架
 */
interface UserRepository {

    /**
     * 当前登录用户的只读状态流，未登录时为 null
     */
    val currentUser: StateFlow<UserInfo?>

    /**
     * 是否已登录
     */
    fun isLoggedIn(): Boolean

    /**
     * 执行登录（挂起函数，内部可包含网络/IO 操作）
     */
    suspend fun login(username: String, password: String): LoginResult

    /**
     * 登出
     */
    fun logout()
}
