package zhaoyun.example.composedemo.story.message.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.message.domain.MessageEffect
import zhaoyun.example.composedemo.story.message.domain.MessageEvent
import zhaoyun.example.composedemo.story.message.domain.MessageState
import zhaoyun.example.composedemo.story.message.domain.MessageUseCase

class MessageViewModel(
    stateHolder: StateHolder<MessageState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<MessageState, MessageEvent, MessageEffect>(
    stateHolder,
    serviceRegistry,
    { holder, registry -> MessageUseCase(stateHolder = holder, serviceRegistry = registry) },
)
