package zhaoyun.example.composedemo.story.infobar.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase

class InfoBarUseCase(
    private val cardId: String,
) : BaseUseCase<InfoBarState, InfoBarEvent, InfoBarEffect>(
    InfoBarState()
) {
    override suspend fun onEvent(event: InfoBarEvent) {
        when (event) {
            is InfoBarEvent.OnLikeClicked -> {
                val newIsLiked = !currentState.isLiked
                val newLikes = if (newIsLiked) currentState.likes + 1 else currentState.likes - 1
                updateState { it.copy(isLiked = newIsLiked, likes = newLikes.coerceAtLeast(0)) }
            }
            is InfoBarEvent.OnShareClicked -> {
                sendEffect(InfoBarEffect.ShowShareSheet(cardId))
            }
            is InfoBarEvent.OnCommentClicked -> {
                sendEffect(InfoBarEffect.NavigateToComments(cardId))
            }
            is InfoBarEvent.OnHistoryClicked -> {
                sendEffect(InfoBarEffect.ShowHistory(cardId))
            }
        }
    }
}
