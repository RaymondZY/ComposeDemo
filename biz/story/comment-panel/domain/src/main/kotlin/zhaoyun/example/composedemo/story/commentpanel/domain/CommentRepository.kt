package zhaoyun.example.composedemo.story.commentpanel.domain

interface CommentRepository {
    suspend fun loadInitial(cardId: String, pageSize: Int): CommentInitialResult
    suspend fun loadMoreComments(cardId: String, cursor: String, pageSize: Int): CommentPage
    suspend fun loadReplies(cardId: String, commentId: String, cursor: String?, pageSize: Int): ReplyPage
    suspend fun setCommentLiked(cardId: String, commentId: String, liked: Boolean): CommentLikeResult
    suspend fun sendComment(cardId: String, content: String): SendCommentResult
}

data class CommentInitialResult(
    val totalCount: Int,
    val dialogueEntry: DialogueEntryState,
    val page: CommentPage,
)

data class CommentPage(
    val comments: List<CommentItem>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

data class ReplyPage(
    val replies: List<ReplyItem>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

data class CommentLikeResult(
    val commentId: String,
    val isLiked: Boolean,
    val likeCount: Int,
)

data class SendCommentResult(
    val comment: CommentItem,
    val totalCount: Int,
)
