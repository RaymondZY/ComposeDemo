package zhaoyun.example.composedemo.todo.presentation

import zhaoyun.example.composedemo.domain.model.TodoEffect
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.model.TodoState
import zhaoyun.example.composedemo.domain.usecase.TodoUseCases
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel

class TodoViewModel(
    useCase: TodoUseCases
) : BaseViewModel<TodoState, TodoEvent, TodoEffect>(useCase)
