package zhaoyun.example.composedemo.story.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase
import zhaoyun.example.composedemo.story.message.domain.MessageAnalytics

class StoryCardUseCase(
    stateHolder: StateHolder<StoryCardState>,
) : BaseUseCase<StoryCardState, StoryCardEvent, StoryCardEffect>(
    stateHolder,
), MessageAnalytics {

    // 实现服务接口
    override fun trackMessageClicked() {
        println("trackMessageClicked")
    }

    override fun trackMessageExpanded(expanded: Boolean) {
        println("trackMessageExpanded expanded = $expanded")
    }

    override suspend fun onEvent(event: StoryCardEvent) {
        // Placeholder - business logic handled by child use-cases
    }
}
