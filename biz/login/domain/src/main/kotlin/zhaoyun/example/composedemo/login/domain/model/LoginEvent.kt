package zhaoyun.example.composedemo.login.domain.model

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class LoginEvent : UiEvent {
    data class OnUsernameChanged(val username: String) : LoginEvent()
    data class OnPasswordChanged(val password: String) : LoginEvent()
    data object OnLoginClicked : LoginEvent()
}
