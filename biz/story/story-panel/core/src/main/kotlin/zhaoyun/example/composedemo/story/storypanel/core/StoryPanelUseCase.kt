package zhaoyun.example.composedemo.story.storypanel.core

import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class StoryPanelUseCase(
    stateHolder: StateHolder<StoryPanelState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<StoryPanelState, StoryPanelEvent, StoryPanelEffect>(
    stateHolder,
    serviceRegistry,
) {
    override suspend fun onEvent(event: StoryPanelEvent) {
        when (event) {
            is StoryPanelEvent.OnDismiss -> {
                dispatchEffect(StoryPanelEffect.NavigateBack)
            }
        }
    }
}
