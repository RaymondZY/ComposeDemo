package zhaoyun.example.composedemo.story.commentpanel.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class CommentPanelUseCase(
    private val cardId: String,
    private val commentRepository: CommentRepository = FakeCommentRepository(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    stateHolder: StateHolder<CommentPanelState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<CommentPanelState, CommentPanelEvent, CommentPanelEffect>(
    stateHolder,
    serviceRegistry,
) {
    private var loadJob: Job? = null
    private var sendJob: Job? = null

    override suspend fun onEvent(event: CommentPanelEvent) {
        when (event) {
            is CommentPanelEvent.OnPanelShown -> loadComments()
            is CommentPanelEvent.OnInputChanged -> updateState { it.copy(inputText = event.text) }
            is CommentPanelEvent.OnSendClicked -> sendComment()
        }
    }

    private fun loadComments() {
        if (currentState.isLoadingComments) return
        loadJob?.cancel()
        updateState { it.copy(cardId = cardId, isLoadingComments = true) }
        loadJob = scope.launch {
            try {
                val comments = commentRepository.loadComments(cardId)
                updateState { it.copy(comments = comments, isLoadingComments = false) }
            } catch (_: Exception) {
                updateState { it.copy(isLoadingComments = false) }
                dispatchBaseEffect(BaseEffect.ShowToast("评论加载失败"))
            }
        }
    }

    private fun sendComment() {
        if (currentState.isSendingComment) return
        val content = currentState.inputText.trim()
        if (content.isBlank()) {
            scope.launch {
                dispatchBaseEffect(BaseEffect.ShowToast("请输入评论内容"))
            }
            return
        }
        updateState { it.copy(isSendingComment = true) }
        sendJob?.cancel()
        sendJob = scope.launch {
            try {
                val comment = commentRepository.sendComment(cardId, content)
                updateState {
                    it.copy(
                        comments = it.comments + comment,
                        inputText = "",
                        isSendingComment = false,
                    )
                }
            } catch (_: Exception) {
                updateState { it.copy(isSendingComment = false) }
                dispatchBaseEffect(BaseEffect.ShowToast("评论发送失败"))
            }
        }
    }
}
