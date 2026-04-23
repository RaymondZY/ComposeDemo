package zhaoyun.example.composedemo.login.domain.di

import org.koin.dsl.module
import zhaoyun.example.composedemo.login.domain.usecase.LoginUseCase

/**
 * Login Domain 层 Koin Module
 *
 * 绑定纯 Kotlin 的 UseCase；依赖通过构造函数注入。
 */
val loginDomainModule = module {
    factory { LoginUseCase(get(), get()) }
}
