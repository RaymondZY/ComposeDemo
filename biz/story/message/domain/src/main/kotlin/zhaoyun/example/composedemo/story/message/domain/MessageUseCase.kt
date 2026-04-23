package zhaoyun.example.composedemo.story.message.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase

class MessageUseCase : BaseUseCase<MessageState, MessageEvent, MessageEffect>(
    MessageState()
) {
    override suspend fun onEvent(event: MessageEvent) {
        when (event) {
            is MessageEvent.OnDialogueClicked -> {
                updateState { it.copy(isExpanded = !it.isExpanded) }
            }
        }
    }
}
