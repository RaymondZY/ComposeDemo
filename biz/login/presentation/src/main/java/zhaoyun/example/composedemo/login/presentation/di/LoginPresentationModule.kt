package zhaoyun.example.composedemo.login.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.login.domain.di.loginDomainModule
import zhaoyun.example.composedemo.login.presentation.LoginViewModel

/**
 * Login Presentation 层 Koin Module
 *
 * 绑定 ViewModel；依赖通过构造函数注入。
 */
val loginPresentationModule = module {
    viewModel { LoginViewModel(get()) }
}

/**
 * 供 Application 层一键导入的完整 Login 模块组合。
 */
val loginModules = listOf(
    loginDomainModule,
    loginPresentationModule
)
