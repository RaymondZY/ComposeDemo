package zhaoyun.example.composedemo.story.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase
import zhaoyun.example.composedemo.scaffold.core.mvi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.mvi.ServiceProvider
import zhaoyun.example.composedemo.scaffold.core.mvi.register
import zhaoyun.example.composedemo.story.message.domain.MessageAnalytics

class StoryCardUseCase : BaseUseCase<StoryCardState, StoryCardEvent, StoryCardEffect>(
    StoryCardState()
), ServiceProvider, MessageAnalytics {

    // 声明：本 UseCase 提供 MessageAnalytics 服务
    override fun provideServices(registry: MutableServiceRegistry) {
        registry.register<MessageAnalytics>(this)
    }

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
