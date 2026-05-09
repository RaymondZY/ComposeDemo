package zhaoyun.example.composedemo.story.commentpanel.domain

import kotlinx.coroutines.CancellationException
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
    private val commentRepository: CommentRepository = FakeCommentRepository(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    stateHolder: StateHolder<CommentPanelState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<CommentPanelState, CommentPanelEvent, CommentPanelEffect>(
    stateHolder,
    serviceRegistry,
) {
    private var initialLoadJob: Job? = null
    private var initialLoadRequestId = 0

    override suspend fun onEvent(event: CommentPanelEvent) {
        when (event) {
            CommentPanelEvent.OnPanelShown -> loadInitialIfNeeded()
            CommentPanelEvent.OnRetryInitialLoad -> loadInitial(force = true)
            CommentPanelEvent.OnLoadMoreComments -> loadMoreComments()
            CommentPanelEvent.OnDialogueEntryClicked -> navigateDialogueIfAvailable()
            is CommentPanelEvent.OnCommentExpanded -> expandComment(event.commentId)
            is CommentPanelEvent.OnCommentLikeClicked -> toggleCommentLike(event.commentId)
            is CommentPanelEvent.OnRepliesExpanded -> expandReplies(event.commentId)
            is CommentPanelEvent.OnRepliesCollapsed -> collapseReplies(event.commentId)
            is CommentPanelEvent.OnLoadMoreReplies -> loadMoreReplies(event.commentId)
            is CommentPanelEvent.OnInputChanged -> updateInput(event.text)
            CommentPanelEvent.OnSendClicked -> sendComment()
        }
    }

    private fun loadInitialIfNeeded() {
        if (currentState.initialLoadStatus == LoadStatus.Idle) {
            loadInitial(force = false)
        }
    }

    private fun loadInitial(force: Boolean) {
        if (!force && currentState.initialLoadStatus == LoadStatus.Loading) return

        val stateBeforeLoad = currentState
        val cardId = stateBeforeLoad.cardId
        val cancellationFallbackStatus = stateBeforeLoad.initialLoadCancellationFallbackStatus()
        val requestId = ++initialLoadRequestId
        initialLoadJob?.cancel()
        updateState {
            it.copy(
                initialLoadStatus = LoadStatus.Loading,
                commentPagination = it.commentPagination.copy(isLoading = false, errorMessage = null),
            )
        }
        initialLoadJob = scope.launch {
            try {
                val result = commentRepository.loadInitial(cardId, CommentPanelInitialPageSize)
                if (requestId != initialLoadRequestId) return@launch
                val comments = result.page.comments.map { it.toCommentItem() }
                updateState {
                    it.copy(
                        totalCount = result.totalCount,
                        dialogueEntry = result.dialogueEntry,
                        comments = comments,
                        initialLoadStatus = if (comments.isEmpty()) LoadStatus.Empty else LoadStatus.Success,
                        commentPagination = result.page.toPaginationState(),
                    )
                }
            } catch (cancellation: CancellationException) {
                if (requestId == initialLoadRequestId) {
                    updateState { it.copy(initialLoadStatus = cancellationFallbackStatus) }
                }
                throw cancellation
            } catch (_: Exception) {
                if (requestId != initialLoadRequestId) return@launch
                updateState { it.copy(initialLoadStatus = LoadStatus.Error) }
                dispatchBaseEffect(BaseEffect.ShowToast("评论加载失败"))
            }
        }
    }

    private fun CommentPanelState.initialLoadCancellationFallbackStatus(): LoadStatus {
        return when (initialLoadStatus) {
            LoadStatus.Loading -> if (comments.isEmpty()) LoadStatus.Idle else LoadStatus.Success
            else -> initialLoadStatus
        }
    }

    private fun loadMoreComments() {
        val pagination = currentState.commentPagination
        val cursor = pagination.nextCursor ?: return
        if (!pagination.hasMore || pagination.isLoading) return

        val cardId = currentState.cardId
        updateState {
            it.copy(
                commentPagination = pagination.copy(isLoading = true, errorMessage = null),
            )
        }
        scope.launch {
            try {
                val page = commentRepository.loadMoreComments(cardId, cursor, CommentPanelInitialPageSize)
                val nextComments = page.comments.map { it.toCommentItem() }
                updateState {
                    it.copy(
                        comments = (it.comments + nextComments).distinctBy { comment -> comment.commentId },
                        commentPagination = page.toPaginationState(),
                    )
                }
            } catch (cancellation: CancellationException) {
                updateState {
                    it.copy(
                        commentPagination = it.commentPagination.copy(
                            isLoading = false,
                            errorMessage = null,
                        ),
                    )
                }
                throw cancellation
            } catch (_: Exception) {
                updateState {
                    it.copy(
                        commentPagination = it.commentPagination.copy(
                            isLoading = false,
                            errorMessage = "评论加载失败",
                        ),
                    )
                }
            }
        }
    }

    private suspend fun navigateDialogueIfAvailable() {
        val entry = currentState.dialogueEntry
        if (entry is DialogueEntryState.Available) {
            dispatchEffect(CommentPanelEffect.NavigateToDialogue(currentState.cardId, entry.targetId))
        }
    }

    private fun expandComment(commentId: String) {
        updateComment(commentId) { it.copy(isExpanded = true) }
    }

    private fun toggleCommentLike(commentId: String) {
        val oldComment = findComment(commentId) ?: return
        if (oldComment.isLikeSubmitting) return

        val targetLiked = !oldComment.isLiked
        val optimisticLikeCount = if (targetLiked) {
            oldComment.likeCount + 1
        } else {
            (oldComment.likeCount - 1).coerceAtLeast(0)
        }
        updateComment(commentId) {
            it.copy(
                likeCount = optimisticLikeCount,
                isLiked = targetLiked,
                isLikeSubmitting = true,
            )
        }

        val cardId = currentState.cardId
        scope.launch {
            try {
                val result = commentRepository.setCommentLiked(cardId, commentId, targetLiked)
                updateComment(commentId) {
                    it.copy(
                        likeCount = result.likeCount.coerceAtLeast(0),
                        isLiked = result.isLiked,
                        isLikeSubmitting = false,
                    )
                }
            } catch (cancellation: CancellationException) {
                updateComment(commentId) { oldComment }
                throw cancellation
            } catch (_: Exception) {
                updateComment(commentId) { oldComment }
                dispatchBaseEffect(BaseEffect.ShowToast("点赞失败，请重试"))
            }
        }
    }

    private fun updateComment(commentId: String, transform: (CommentItem) -> CommentItem) {
        updateState { state ->
            state.copy(
                comments = state.comments.map { comment ->
                    if (comment.commentId == commentId) transform(comment) else comment
                },
            )
        }
    }

    private fun findComment(commentId: String): CommentItem? {
        return currentState.comments.firstOrNull { it.commentId == commentId }
    }

    private fun expandReplies(commentId: String) {
        val comment = findComment(commentId) ?: return
        val replySection = comment.replySection
        if (replySection.isLoading) return
        if (replySection.replies.isNotEmpty()) {
            updateComment(commentId) {
                it.copy(replySection = it.replySection.copy(isExpanded = true))
            }
            return
        }

        loadReplies(commentId, cursor = null)
    }

    private fun collapseReplies(commentId: String) {
        updateComment(commentId) {
            it.copy(replySection = it.replySection.copy(isExpanded = false))
        }
    }

    private fun loadMoreReplies(commentId: String) {
        val comment = findComment(commentId) ?: return
        val replySection = comment.replySection
        val cursor = replySection.pagination.nextCursor ?: return
        if (!replySection.isExpanded || replySection.isLoading || !replySection.pagination.hasMore) return

        loadReplies(commentId, cursor)
    }

    private fun loadReplies(commentId: String, cursor: String?) {
        val comment = findComment(commentId) ?: return
        if (comment.replySection.isLoading) return

        val cardId = currentState.cardId
        updateComment(commentId) {
            it.copy(
                replySection = it.replySection.copy(
                    isExpanded = true,
                    isLoading = true,
                    errorMessage = null,
                ),
            )
        }
        scope.launch {
            try {
                val page = commentRepository.loadReplies(cardId, commentId, cursor, CommentPanelReplyPageSize)
                val nextReplies = page.replies.map { it.toReplyItem() }
                updateComment(commentId) {
                    it.copy(
                        replySection = it.replySection.copy(
                            isExpanded = true,
                            isLoading = false,
                            replies = (it.replySection.replies + nextReplies).distinctBy { reply -> reply.replyId },
                            pagination = page.toPaginationState(),
                            errorMessage = null,
                        ),
                    )
                }
            } catch (cancellation: CancellationException) {
                updateComment(commentId) {
                    it.copy(
                        replySection = it.replySection.copy(
                            isLoading = false,
                            errorMessage = null,
                        ),
                    )
                }
                throw cancellation
            } catch (_: Exception) {
                updateComment(commentId) {
                    it.copy(
                        replySection = it.replySection.copy(
                            isLoading = false,
                            errorMessage = "回复加载失败",
                        ),
                    )
                }
            }
        }
    }

    private fun updateInput(text: String) {
        updateState {
            it.copy(
                inputText = text,
                inputErrorMessage = null,
                sendErrorMessage = null,
            )
        }
    }

    private fun sendComment() = Unit

    private fun CommentPage.toPaginationState(): PaginationState = PaginationState(
        nextCursor = nextCursor,
        hasMore = hasMore,
        isLoading = false,
        errorMessage = null,
    )

    private fun ReplyPage.toPaginationState(): PaginationState = PaginationState(
        nextCursor = nextCursor,
        hasMore = hasMore,
        isLoading = false,
        errorMessage = null,
    )
}
