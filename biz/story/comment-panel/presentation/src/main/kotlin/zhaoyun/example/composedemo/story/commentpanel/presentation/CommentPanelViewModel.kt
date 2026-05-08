package zhaoyun.example.composedemo.story.commentpanel.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelEffect
import zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelEvent
import zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelState
import zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCase

class CommentPanelViewModel(
    cardId: String,
    stateHolder: StateHolder<CommentPanelState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<CommentPanelState, CommentPanelEvent, CommentPanelEffect>(
    stateHolder,
    serviceRegistry,
    { holder, registry ->
        CommentPanelUseCase(
            cardId = cardId,
            stateHolder = holder,
            serviceRegistry = registry,
        )
    },
)
