package zhaoyun.example.composedemo.story.message.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.story.message.domain.MessageEffect
import zhaoyun.example.composedemo.story.message.domain.MessageEvent
import zhaoyun.example.composedemo.story.message.domain.MessageState
import zhaoyun.example.composedemo.story.message.domain.MessageUseCase

class MessageViewModel(
    stateHolder: StateHolder<MessageState>? = null,
    parentServiceRegistry: ServiceRegistry? = null,
) : BaseViewModel<MessageState, MessageEvent, MessageEffect>(
    MessageState(),
    { holder -> MessageUseCase(stateHolder = holder) },
    stateHolder = stateHolder,
    parentServiceRegistry = parentServiceRegistry,
)
