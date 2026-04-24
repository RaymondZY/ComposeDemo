package zhaoyun.example.composedemo.todo.presentation

import zhaoyun.example.composedemo.domain.model.TodoEffect
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.model.TodoState
import zhaoyun.example.composedemo.domain.usecase.CheckLoginUseCase
import zhaoyun.example.composedemo.domain.usecase.TodoUseCases
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder

class TodoViewModel(
    todoUseCases: TodoUseCases,
    checkLoginUseCase: CheckLoginUseCase,
    injectedStateHolder: StateHolder<TodoState>? = null
) : BaseViewModel<TodoState, TodoEvent, TodoEffect>(
    initialState = TodoState(),
    injectedStateHolder,
    todoUseCases,
    checkLoginUseCase
)
