package zhaoyun.example.composedemo.login.domain.model

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect

sealed class LoginEffect : UiEffect {
    data object NavigateToHome : LoginEffect()
}
