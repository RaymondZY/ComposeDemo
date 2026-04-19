package zhaoyun.example.composedemo.service.usercenter.api.model

sealed class LoginResult {
    data class Success(val userInfo: UserInfo) : LoginResult()
    data class Failure(val message: String) : LoginResult()
}
