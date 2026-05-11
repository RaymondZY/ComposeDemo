package zhaoyun.example.composedemo.story.background.platform

import zhaoyun.example.composedemo.scaffold.platform.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.background.core.BackgroundEffect
import zhaoyun.example.composedemo.story.background.core.BackgroundEvent
import zhaoyun.example.composedemo.story.background.core.BackgroundState
import zhaoyun.example.composedemo.story.background.core.BackgroundUseCase

class BackgroundViewModel(
    stateHolder: StateHolder<BackgroundState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<BackgroundState, BackgroundEvent, BackgroundEffect>(
    stateHolder,
    serviceRegistry,
    { holder, registry -> BackgroundUseCase(stateHolder = holder, serviceRegistry = registry) },
)
