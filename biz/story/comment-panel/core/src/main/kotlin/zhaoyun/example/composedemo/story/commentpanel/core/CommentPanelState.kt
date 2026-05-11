package zhaoyun.example.composedemo.story.commentpanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

private const val DefaultPageSize = 20
const val CommentPanelMaxInputLength = 200
const val CommentPanelInitialPageSize = DefaultPageSize
const val CommentPanelReplyPageSize = 10

data class CommentPanelState(
    val cardId: String = "",
    val totalCount: Int = 0,
    val dialogueEntry: DialogueEntryState = DialogueEntryState.Hidden,
    val comments: List<CommentItem> = emptyList(),
    val initialLoadStatus: LoadStatus = LoadStatus.Idle,
    val commentPagination: PaginationState = PaginationState(),
    val inputText: String = "",
    val isSendingComment: Boolean = false,
    val inputErrorMessage: String? = null,
    val sendErrorMessage: String? = null,
) : UiState

enum class LoadStatus {
    Idle,
    Loading,
    Success,
    Empty,
    Error,
}

data class PaginationState(
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed class DialogueEntryState {
    data object Hidden : DialogueEntryState()
    data class Available(
        val title: String,
        val description: String,
        val targetId: String,
    ) : DialogueEntryState()
    data class Unavailable(
        val reason: String,
    ) : DialogueEntryState()
}

data class CommentUser(
    val userId: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val isAuthor: Boolean = false,
)

data class CommentItem(
    val commentId: String,
    val user: CommentUser,
    val content: String,
    val createdAtText: String,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val isLikeSubmitting: Boolean = false,
    val isPinned: Boolean = false,
    val canExpand: Boolean = false,
    val isExpanded: Boolean = false,
    val replyCount: Int = 0,
    val replySection: ReplySectionState = ReplySectionState(),
)

data class ReplySectionState(
    val isExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val replies: List<ReplyItem> = emptyList(),
    val pagination: PaginationState = PaginationState(),
    val errorMessage: String? = null,
)

data class ReplyItem(
    val replyId: String,
    val parentCommentId: String,
    val user: CommentUser,
    val content: String,
    val createdAtText: String,
)
