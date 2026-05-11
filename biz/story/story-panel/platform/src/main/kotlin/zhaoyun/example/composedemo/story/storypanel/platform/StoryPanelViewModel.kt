package zhaoyun.example.composedemo.story.storypanel.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.storypanel.domain.StoryPanelEffect
import zhaoyun.example.composedemo.story.storypanel.domain.StoryPanelEvent
import zhaoyun.example.composedemo.story.storypanel.domain.StoryPanelState
import zhaoyun.example.composedemo.story.storypanel.domain.StoryPanelUseCase

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
