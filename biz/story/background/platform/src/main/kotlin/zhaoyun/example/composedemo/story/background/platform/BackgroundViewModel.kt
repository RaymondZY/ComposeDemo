package zhaoyun.example.composedemo.story.background.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.background.domain.BackgroundEffect
import zhaoyun.example.composedemo.story.background.domain.BackgroundEvent
import zhaoyun.example.composedemo.story.background.domain.BackgroundState
import zhaoyun.example.composedemo.story.background.domain.BackgroundUseCase

class BackgroundViewModel(
    stateHolder: StateHolder<BackgroundState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<BackgroundState, BackgroundEvent, BackgroundEffect>(
    stateHolder,
    serviceRegistry,
    { holder, registry -> BackgroundUseCase(stateHolder = holder, serviceRegistry = registry) },
)
