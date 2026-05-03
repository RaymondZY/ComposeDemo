package zhaoyun.example.composedemo.story.infobar.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class InfoBarUseCase(
    private val cardId: String,
    private val likeRepository: LikeRepository = FakeLikeRepository(),
    stateHolder: StateHolder<InfoBarState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<InfoBarState, InfoBarEvent, InfoBarEffect>(
    stateHolder,
    serviceRegistry,
) {
    override suspend fun onEvent(event: InfoBarEvent) {
        when (event) {
            is InfoBarEvent.OnLikeClicked -> {
                val newIsLiked = !currentState.isLiked
                val newLikes = if (newIsLiked) currentState.likes + 1 else currentState.likes - 1
                updateState { it.copy(isLiked = newIsLiked, likes = newLikes.coerceAtLeast(0)) }

                val result = likeRepository.toggleLike(cardId, newIsLiked)
                updateState { it.copy(isLiked = result.isLiked, likes = result.likes.coerceAtLeast(0)) }
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

            is InfoBarEvent.OnCreatorClicked -> {
                dispatchEffect(InfoBarEffect.NavigateToCreatorProfile(currentState.creatorHandle))
            }
        }
    }
}
