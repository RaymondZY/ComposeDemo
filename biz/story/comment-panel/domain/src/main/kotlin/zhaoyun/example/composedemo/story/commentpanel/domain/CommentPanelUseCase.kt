package zhaoyun.example.composedemo.story.commentpanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class CommentPanelUseCase(
    stateHolder: StateHolder<CommentPanelState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<CommentPanelState, CommentPanelEvent, CommentPanelEffect>(
    stateHolder,
    serviceRegistry,
) {
    override suspend fun onEvent(event: CommentPanelEvent) = Unit
}
