package zhaoyun.example.composedemo.login.presentation

import zhaoyun.example.composedemo.login.domain.model.LoginEffect
import zhaoyun.example.composedemo.login.domain.model.LoginEvent
import zhaoyun.example.composedemo.login.domain.model.LoginState
import zhaoyun.example.composedemo.login.domain.usecase.LoginUseCase
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer

class LoginViewModel(
    useCase: LoginUseCase,
    injectedReducer: Reducer<LoginState>? = null
) : BaseViewModel<LoginState, LoginEvent, LoginEffect>(
    initialState = LoginState(),
    injectedReducer,
    useCase
)
