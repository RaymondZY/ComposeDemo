package zhaoyun.example.composedemo.todo.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.domain.di.todoDomainModule
import zhaoyun.example.composedemo.todo.presentation.TodoViewModel

/**
 * Todo List Presentation 层 Koin Module
 *
 * 绑定 ViewModel；依赖通过构造函数注入。
 * 并聚合 Domain 层的 Module。
 */
val todoPresentationModule = module {
    viewModel { TodoViewModel(get()) }
}

/**
 * 供 Application 层一键导入的完整 Todo List 模块组合。
 */
val todoModules = listOf(
    todoDomainModule,
    todoPresentationModule
)
