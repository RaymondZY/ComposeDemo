package zhaoyun.example.composedemo.login.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.login.domain.di.loginDomainModule
import zhaoyun.example.composedemo.login.domain.model.LoginState
import zhaoyun.example.composedemo.login.presentation.LoginViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer

/**
 * Login Presentation 层 Koin Module
 *
 * 绑定 ViewModel；依赖通过构造函数注入。
 * 支持可选注入外部 [Reducer<LoginState>]，用于 Global 嵌入模式。
 */
val loginPresentationModule = module {
    viewModel { params ->
        LoginViewModel(
            get(),
            params.getOrNull<Reducer<LoginState>>()
        )
    }
}

/**
 * 供 Application 层一键导入的完整 Login 模块组合。
 */
val loginModules = listOf(
    loginDomainModule,
    loginPresentationModule
)
