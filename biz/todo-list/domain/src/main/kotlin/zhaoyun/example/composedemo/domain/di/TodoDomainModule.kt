package zhaoyun.example.composedemo.domain.di

import org.koin.dsl.module
import zhaoyun.example.composedemo.domain.usecase.CheckLoginUseCase
import zhaoyun.example.composedemo.domain.usecase.TodoUseCases

/**
 * Todo List Domain 层 Koin Module
 *
 * 绑定纯 Kotlin 的 UseCase；实例内部通过 Koin [inject] 字段注入依赖。
 */
val todoDomainModule = module {
    factory { CheckLoginUseCase() }
    factory { TodoUseCases() }
}
