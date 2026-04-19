package zhaoyun.example.composedemo.login.domain.model

sealed class LoginEffect {
    data object NavigateToHome : LoginEffect()
    data class ShowToast(val message: String) : LoginEffect()
}
