package zhaoyun.example.composedemo.login.presentation

import zhaoyun.example.composedemo.login.domain.model.LoginEffect
import zhaoyun.example.composedemo.login.domain.model.LoginEvent
import zhaoyun.example.composedemo.login.domain.model.LoginState
import zhaoyun.example.composedemo.login.domain.usecase.LoginUseCase
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel

class LoginViewModel(
    useCase: LoginUseCase
) : BaseViewModel<LoginState, LoginEvent, LoginEffect>(
    initialState = LoginState(),
    useCase
)
