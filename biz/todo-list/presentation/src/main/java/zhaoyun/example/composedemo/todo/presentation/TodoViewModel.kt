package zhaoyun.example.composedemo.todo.presentation

import zhaoyun.example.composedemo.domain.model.TodoEffect
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.model.TodoState
import zhaoyun.example.composedemo.domain.usecase.CheckLoginUseCase
import zhaoyun.example.composedemo.domain.usecase.TodoUseCases
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer

class TodoViewModel(
    todoUseCases: TodoUseCases,
    checkLoginUseCase: CheckLoginUseCase,
    injectedReducer: Reducer<TodoState>? = null
) : BaseViewModel<TodoState, TodoEvent, TodoEffect>(
    initialState = TodoState(),
    injectedReducer,
    todoUseCases,
    checkLoginUseCase
)
