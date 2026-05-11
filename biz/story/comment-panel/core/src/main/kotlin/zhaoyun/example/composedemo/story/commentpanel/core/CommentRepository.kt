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
    val comments: List<CommentData>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

data class ReplyPage(
    val replies: List<ReplyData>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

data class CommentLikeResult(
    val commentId: String,
    val isLiked: Boolean,
    val likeCount: Int,
)

data class SendCommentResult(
    val comment: CommentData,
    val totalCount: Int,
)

data class CommentData(
    val commentId: String,
    val user: CommentUser,
    val content: String,
    val createdAtText: String,
    val likeCount: Int,
    val isLiked: Boolean,
    val isPinned: Boolean,
    val canExpand: Boolean,
    val replyCount: Int,
)

data class ReplyData(
    val replyId: String,
    val parentCommentId: String,
    val user: CommentUser,
    val content: String,
    val createdAtText: String,
)

fun CommentData.toCommentItem(): CommentItem = CommentItem(
    commentId = commentId,
    user = user,
    content = content,
    createdAtText = createdAtText,
    likeCount = likeCount,
    isLiked = isLiked,
    isLikeSubmitting = false,
    isPinned = isPinned,
    canExpand = canExpand,
    isExpanded = false,
    replyCount = replyCount,
    replySection = ReplySectionState(),
)

fun ReplyData.toReplyItem(): ReplyItem = ReplyItem(
    replyId = replyId,
    parentCommentId = parentCommentId,
    user = user,
    content = content,
    createdAtText = createdAtText,
)
