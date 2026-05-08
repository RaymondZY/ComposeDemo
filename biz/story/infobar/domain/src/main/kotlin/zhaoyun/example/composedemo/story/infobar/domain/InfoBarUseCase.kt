package zhaoyun.example.composedemo.story.infobar.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class InfoBarUseCase(
    private val cardId: String,
    private val likeRepository: LikeRepository = FakeLikeRepository(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    stateHolder: StateHolder<InfoBarState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<InfoBarState, InfoBarEvent, InfoBarEffect>(
    stateHolder,
    serviceRegistry,
) {
    private var likeJob: Job? = null

    override suspend fun onEvent(event: InfoBarEvent) {
        when (event) {
            is InfoBarEvent.OnLikeClicked -> {
                val oldState = currentState
                val newIsLiked = !oldState.isLiked
                val newLikes = if (newIsLiked) oldState.likes + 1 else oldState.likes - 1
                updateState { it.copy(isLiked = newIsLiked, likes = newLikes.coerceAtLeast(0)) }

                likeJob?.cancel()
                likeJob = scope.launch {
                    try {
                        val result = likeRepository.toggleLike(cardId, newIsLiked, oldState.likes)
                        updateState { it.copy(isLiked = result.isLiked, likes = result.likes.coerceAtLeast(0)) }
                    } catch (_: Exception) {
                        updateState { oldState }
                        dispatchBaseEffect(BaseEffect.ShowToast("操作失败，请重试"))
                    }
                }
            }

            is InfoBarEvent.OnShareClicked -> {
                dispatchEffect(InfoBarEffect.OpenSharePanel(cardId))
            }

            is InfoBarEvent.OnCommentClicked -> {
                dispatchEffect(InfoBarEffect.NavigateToComments(cardId))
            }

            is InfoBarEvent.OnHistoryClicked -> {
                dispatchEffect(InfoBarEffect.ShowHistory(cardId))
            }

            is InfoBarEvent.OnStoryTitleClicked -> {
                dispatchEffect(InfoBarEffect.NavigateToStoryDetail(cardId))
            }
        }
    }
}
