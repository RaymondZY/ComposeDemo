package zhaoyun.example.composedemo.story.message.platform

import zhaoyun.example.composedemo.scaffold.platform.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.message.core.MessageEffect
import zhaoyun.example.composedemo.story.message.core.MessageEvent
import zhaoyun.example.composedemo.story.message.core.MessageState
import zhaoyun.example.composedemo.story.message.core.MessageUseCase

class MessageViewModel(
    stateHolder: StateHolder<MessageState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<MessageState, MessageEvent, MessageEffect>(
    stateHolder,
    serviceRegistry,
    { holder, registry -> MessageUseCase(stateHolder = holder, serviceRegistry = registry) },
)
