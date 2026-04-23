package zhaoyun.example.composedemo.story.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase

class StoryCardUseCase : BaseUseCase<StoryCardState, StoryCardEvent, StoryCardEffect>(
    StoryCardState()
) {
    override suspend fun onEvent(event: StoryCardEvent) {
        // Placeholder - business logic handled by child use-cases
    }
}
