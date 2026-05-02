package zhaoyun.example.composedemo.story.message.domain

import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder

class MessageUseCase(
    stateHolder: StateHolder<MessageState>,
) : BaseUseCase<MessageState, MessageEvent, MessageEffect>(
    stateHolder,
) {
    override suspend fun onEvent(event: MessageEvent) {
        when (event) {
            is MessageEvent.OnDialogueClicked -> {
                // 自动在同 Screen 的 registry 中找到 MessageAnalytics 实现
                findService<MessageAnalytics>().trackMessageClicked()
                updateState { it.copy(isExpanded = !it.isExpanded) }
            }
        }
    }
}
