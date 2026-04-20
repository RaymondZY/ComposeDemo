package zhaoyun.example.composedemo.login.domain.model

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class LoginState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : UiState
