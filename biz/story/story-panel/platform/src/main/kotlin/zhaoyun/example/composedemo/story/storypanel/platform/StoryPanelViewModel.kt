package zhaoyun.example.composedemo.story.storypanel.platform

import zhaoyun.example.composedemo.scaffold.platform.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.storypanel.core.StoryPanelEffect
import zhaoyun.example.composedemo.story.storypanel.core.StoryPanelEvent
import zhaoyun.example.composedemo.story.storypanel.core.StoryPanelState
import zhaoyun.example.composedemo.story.storypanel.core.StoryPanelUseCase

class StoryPanelViewModel(
    stateHolder: StateHolder<StoryPanelState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<StoryPanelState, StoryPanelEvent, StoryPanelEffect>(
    stateHolder,
    serviceRegistry,
    { holder, registry ->
        StoryPanelUseCase(
            stateHolder = holder,
            serviceRegistry = registry,
        )
    },
)
