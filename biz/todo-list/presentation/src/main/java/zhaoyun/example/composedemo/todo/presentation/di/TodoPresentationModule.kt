package zhaoyun.example.composedemo.todo.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.domain.di.todoDomainModule
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer
import zhaoyun.example.composedemo.domain.model.TodoState
import zhaoyun.example.composedemo.todo.presentation.TodoViewModel

/**
 * Todo List Presentation 层 Koin Module
 *
 * 绑定 ViewModel；依赖通过构造函数注入。
 * 支持可选注入外部 [Reducer<TodoState>]，用于 Global 嵌入模式。
 * 并聚合 Domain 层的 Module。
 */
val todoPresentationModule = module {
    viewModel { params ->
        TodoViewModel(
            get(),
            get(),
            params.getOrNull<Reducer<TodoState>>()
        )
    }
}

/**
 * 供 Application 层一键导入的完整 Todo List 模块组合。
 */
val todoModules = listOf(
    todoDomainModule,
    todoPresentationModule
)
