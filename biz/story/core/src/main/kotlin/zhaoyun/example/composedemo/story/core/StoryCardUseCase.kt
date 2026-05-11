package zhaoyun.example.composedemo.story.core

import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class StoryCardUseCase(
    stateHolder: StateHolder<StoryCardState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<StoryCardState, StoryCardEvent, StoryCardEffect>(
    stateHolder,
    serviceRegistry,
) {
    override suspend fun onEvent(event: StoryCardEvent) {
        // Placeholder - business logic handled by child use-cases
    }
}
