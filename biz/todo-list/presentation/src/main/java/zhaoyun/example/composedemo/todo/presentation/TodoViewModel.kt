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
    private val injectedReducer: Reducer<TodoState>? = null
) : BaseViewModel<TodoState, TodoEvent, TodoEffect>(
    initialState = TodoState(),
    todoUseCases,
    checkLoginUseCase
) {
    override fun createReducer(initialState: TodoState): Reducer<TodoState> {
        return injectedReducer ?: super.createReducer(initialState)
    }
}
