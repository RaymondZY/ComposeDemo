package zhaoyun.example.composedemo.story.input.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer
import zhaoyun.example.composedemo.story.input.domain.InputEffect
import zhaoyun.example.composedemo.story.input.domain.InputEvent
import zhaoyun.example.composedemo.story.input.domain.InputState
import zhaoyun.example.composedemo.story.input.domain.InputUseCase

class InputViewModel(
    inputReducer: Reducer<InputState>,
) : BaseViewModel<InputState, InputEvent, InputEffect>(
    InputState(),
    inputReducer,
    InputUseCase()
)
