package zhaoyun.example.composedemo.story.infobar.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class InfoBarUseCase(
    private val cardId: String,
    stateHolder: StateHolder<InfoBarState>,
) : BaseUseCase<InfoBarState, InfoBarEvent, InfoBarEffect>(
    stateHolder,
) {
    override suspend fun onEvent(event: InfoBarEvent) {
        when (event) {
            is InfoBarEvent.OnLikeClicked -> {
                val newIsLiked = !currentState.isLiked
                val newLikes = if (newIsLiked) currentState.likes + 1 else currentState.likes - 1
                updateState { it.copy(isLiked = newIsLiked, likes = newLikes.coerceAtLeast(0)) }
            }

            is InfoBarEvent.OnShareClicked -> {
                dispatchEffect(InfoBarEffect.ShowShareSheet(cardId))
            }

            is InfoBarEvent.OnCommentClicked -> {
                dispatchEffect(InfoBarEffect.NavigateToComments(cardId))
            }

            is InfoBarEvent.OnHistoryClicked -> {
                dispatchEffect(InfoBarEffect.ShowHistory(cardId))
            }
        }
    }
}
