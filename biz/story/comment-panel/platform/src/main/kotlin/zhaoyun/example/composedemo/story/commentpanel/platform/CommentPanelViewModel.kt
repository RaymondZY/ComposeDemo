package zhaoyun.example.composedemo.story.commentpanel.platform

import zhaoyun.example.composedemo.scaffold.platform.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.commentpanel.core.CommentPanelEffect
import zhaoyun.example.composedemo.story.commentpanel.core.CommentPanelEvent
import zhaoyun.example.composedemo.story.commentpanel.core.CommentPanelState
import zhaoyun.example.composedemo.story.commentpanel.core.CommentPanelUseCase

class CommentPanelViewModel(
    stateHolder: StateHolder<CommentPanelState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<CommentPanelState, CommentPanelEvent, CommentPanelEffect>(
    stateHolder,
    serviceRegistry,
    { holder, registry ->
        CommentPanelUseCase(
            stateHolder = holder,
            serviceRegistry = registry,
        )
    },
)
