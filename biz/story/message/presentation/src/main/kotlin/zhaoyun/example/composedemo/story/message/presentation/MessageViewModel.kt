package zhaoyun.example.composedemo.story.message.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer
import zhaoyun.example.composedemo.story.message.domain.MessageEffect
import zhaoyun.example.composedemo.story.message.domain.MessageEvent
import zhaoyun.example.composedemo.story.message.domain.MessageState
import zhaoyun.example.composedemo.story.message.domain.MessageUseCase

class MessageViewModel(
    private val messageReducer: Reducer<MessageState>,
) : BaseViewModel<MessageState, MessageEvent, MessageEffect>(
    MessageState(),
    MessageUseCase()
) {
    override fun createReducer(initialState: MessageState): Reducer<MessageState> =
        messageReducer
}
