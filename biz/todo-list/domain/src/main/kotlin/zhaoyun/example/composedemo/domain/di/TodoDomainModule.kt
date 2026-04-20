package zhaoyun.example.composedemo.domain.di

import org.koin.dsl.module
import zhaoyun.example.composedemo.domain.usecase.CheckLoginUseCase
import zhaoyun.example.composedemo.domain.usecase.TodoUseCases

/**
 * Todo List Domain 层 Koin Module
 *
 * 绑定纯 Kotlin 的 UseCase；依赖通过构造函数注入。
 */
val todoDomainModule = module {
    factory { CheckLoginUseCase(get()) }
    factory { TodoUseCases() }
}
