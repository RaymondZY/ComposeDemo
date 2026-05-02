package zhaoyun.example.composedemo.story.domain

import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.message.domain.MessageAnalytics

class StoryCardUseCase(
    stateHolder: StateHolder<StoryCardState>? = null,
) : BaseUseCase<StoryCardState, StoryCardEvent, StoryCardEffect>(
    initialState = StoryCardState(),
    stateHolder = stateHolder,
), MessageAnalytics {

    // 实现服务接口
    override fun trackMessageClicked() {
        // TODO: 接入实际埋点 SDK
    }

    override fun trackMessageExpanded(expanded: Boolean) {
        // TODO: 接入实际埋点 SDK
    }

    override suspend fun onEvent(event: StoryCardEvent) {
        // Placeholder - business logic handled by child use-cases
    }
}
