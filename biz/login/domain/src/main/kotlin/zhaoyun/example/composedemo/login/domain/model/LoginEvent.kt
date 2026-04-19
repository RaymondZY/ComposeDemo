package zhaoyun.example.composedemo.login.domain.model

sealed class LoginEvent {
    data class OnUsernameChanged(val username: String) : LoginEvent()
    data class OnPasswordChanged(val password: String) : LoginEvent()
    data object OnLoginClicked : LoginEvent()
}
