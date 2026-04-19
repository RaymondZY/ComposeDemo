package zhaoyun.example.composedemo.todo.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.domain.di.todoDomainModule
import zhaoyun.example.composedemo.todo.presentation.TodoViewModel

/**
 * Todo List Presentation 层 Koin Module
 *
 * 绑定 ViewModel；实例内部通过 Koin [inject] 字段注入依赖。
 * 并聚合 Domain 层的 Module。
 */
val todoPresentationModule = module {
    viewModel { TodoViewModel() }
}

/**
 * 供 Application 层一键导入的完整 Todo List 模块组合。
 */
val todoModules = listOf(
    todoDomainModule,
    todoPresentationModule
)
