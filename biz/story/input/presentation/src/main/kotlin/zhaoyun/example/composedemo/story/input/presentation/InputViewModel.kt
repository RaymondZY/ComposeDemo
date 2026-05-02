package zhaoyun.example.composedemo.story.input.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.story.input.domain.InputEffect
import zhaoyun.example.composedemo.story.input.domain.InputEvent
import zhaoyun.example.composedemo.story.input.domain.InputState
import zhaoyun.example.composedemo.story.input.domain.InputUseCase

class InputViewModel(
    stateHolder: StateHolder<InputState>? = null,
    parentServiceRegistry: ServiceRegistry? = null,
) : BaseViewModel<InputState, InputEvent, InputEffect>(
    InputState(),
    { holder -> InputUseCase(stateHolder = holder) },
    stateHolder = stateHolder,
    parentServiceRegistry = parentServiceRegistry,
)
