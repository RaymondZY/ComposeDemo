package zhaoyun.example.composedemo.story.input.platform

import zhaoyun.example.composedemo.scaffold.platform.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.input.core.InputEffect
import zhaoyun.example.composedemo.story.input.core.InputEvent
import zhaoyun.example.composedemo.story.input.core.InputState
import zhaoyun.example.composedemo.story.input.core.InputUseCase

class InputViewModel(
    stateHolder: StateHolder<InputState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<InputState, InputEvent, InputEffect>(
    stateHolder,
    serviceRegistry,
    { holder, registry -> InputUseCase(holder, registry) },
)
