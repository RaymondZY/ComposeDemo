package zhaoyun.example.composedemo.story.message.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.findService
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class MessageUseCase(
    stateHolder: StateHolder<MessageState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<MessageState, MessageEvent, MessageEffect>(
    stateHolder,
    serviceRegistry,
) {
    override suspend fun onEvent(event: MessageEvent) {
        when (event) {
            is MessageEvent.OnDialogueClicked -> {
                // 自动在同 Screen 的 registry 中找到 MessageAnalytics 实现
                findService<MessageAnalytics>()?.trackMessageClicked()
                updateState { it.copy(isExpanded = !it.isExpanded) }
            }
        }
    }
}
