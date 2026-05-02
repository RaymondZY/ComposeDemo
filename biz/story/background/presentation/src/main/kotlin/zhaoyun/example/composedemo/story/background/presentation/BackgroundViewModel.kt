package zhaoyun.example.composedemo.story.background.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.story.background.domain.BackgroundEffect
import zhaoyun.example.composedemo.story.background.domain.BackgroundEvent
import zhaoyun.example.composedemo.story.background.domain.BackgroundState
import zhaoyun.example.composedemo.story.background.domain.BackgroundUseCase

class BackgroundViewModel(
    stateHolder: StateHolder<BackgroundState>? = null,
    parentServiceRegistry: ServiceRegistry? = null,
) : BaseViewModel<BackgroundState, BackgroundEvent, BackgroundEffect>(
    BackgroundState(),
    { holder -> BackgroundUseCase(stateHolder = holder) },
    stateHolder = stateHolder,
    parentServiceRegistry = parentServiceRegistry,
)
