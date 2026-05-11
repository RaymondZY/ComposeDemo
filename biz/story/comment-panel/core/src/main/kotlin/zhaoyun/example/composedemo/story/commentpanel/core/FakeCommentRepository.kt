package zhaoyun.example.composedemo.story.commentpanel.domain

class FakeCommentRepository : CommentRepository {
    private val users = listOf(
        CommentUser("author-1", "作者小云", "https://example.com/a.png", isAuthor = true),
        CommentUser("user-1", "读者一号", "https://example.com/u1.png"),
        CommentUser("user-2", "读者二号", "https://example.com/u2.png"),
    )

    private val comments = listOf(
        createComment("comment-1", users[1], "这个故事很有意思，想继续看后续。", 12, replyCount = 3, isPinned = true),
        createComment("comment-2", users[2], "这个角色的台词很有画面感。", 5, replyCount = 0),
        createComment("comment-3", users[0], "后续剧情会继续更新。", 21, replyCount = 1),
        createComment("comment-4", users[1], "这一段对话可以展开讲讲。", 1, replyCount = 0),
        createComment("comment-5", users[2], "期待更多互动剧情。", 7, replyCount = 2),
    )

    private val replies = mapOf(
        "comment-1" to listOf(
            ReplyData("reply-1", "comment-1", users[0], "谢谢喜欢。", "刚刚"),
            ReplyData("reply-2", "comment-1", users[2], "我也想看后续。", "1分钟前"),
            ReplyData("reply-3", "comment-1", users[1], "已经收藏了。", "2分钟前"),
        ),
        "comment-3" to listOf(
            ReplyData("reply-4", "comment-3", users[1], "坐等更新。", "3分钟前"),
        ),
        "comment-5" to listOf(
            ReplyData("reply-5", "comment-5", users[0], "会加互动。", "5分钟前"),
            ReplyData("reply-6", "comment-5", users[2], "太好了。", "6分钟前"),
        ),
    )

    override suspend fun loadInitial(cardId: String, pageSize: Int): CommentInitialResult {
        return CommentInitialResult(
            totalCount = comments.size,
            dialogueEntry = DialogueEntryState.Available(
                title = "进入对话剧情",
                description = "和角色继续聊下去",
                targetId = "$cardId-dialogue",
            ),
            page = page(comments, start = 0, pageSize = pageSize),
        )
    }

    override suspend fun loadMoreComments(cardId: String, cursor: String, pageSize: Int): CommentPage {
        return page(comments, start = cursor.removePrefix("cursor-").toInt(), pageSize = pageSize)
    }

    override suspend fun loadReplies(cardId: String, commentId: String, cursor: String?, pageSize: Int): ReplyPage {
        val allReplies = replies[commentId].orEmpty()
        val start = cursor?.removePrefix("cursor-")?.toInt() ?: 0
        val end = minOf(start + pageSize, allReplies.size)
        return ReplyPage(
            replies = allReplies.subList(start, end),
            nextCursor = if (end < allReplies.size) "cursor-$end" else null,
            hasMore = end < allReplies.size,
        )
    }

    override suspend fun setCommentLiked(cardId: String, commentId: String, liked: Boolean): CommentLikeResult {
        val comment = comments.first { it.commentId == commentId }
        val likeCount = if (liked) comment.likeCount + 1 else (comment.likeCount - 1).coerceAtLeast(0)
        return CommentLikeResult(commentId, liked, likeCount)
    }

    override suspend fun sendComment(cardId: String, content: String): SendCommentResult {
        return SendCommentResult(
            comment = createComment("local-$cardId-$content", users[0], content, likeCount = 0, replyCount = 0),
            totalCount = comments.size + 1,
        )
    }

    private fun createComment(
        id: String,
        user: CommentUser,
        content: String,
        likeCount: Int,
        replyCount: Int,
        isLiked: Boolean = false,
        isPinned: Boolean = false,
    ): CommentData = CommentData(
        commentId = id,
        user = user,
        content = content,
        createdAtText = "刚刚",
        likeCount = likeCount,
        isLiked = isLiked,
        isPinned = isPinned,
        canExpand = content.length > 18,
        replyCount = replyCount,
    )

    private fun page(items: List<CommentData>, start: Int, pageSize: Int): CommentPage {
        val end = minOf(start + pageSize, items.size)
        return CommentPage(
            comments = items.subList(start, end),
            nextCursor = if (end < items.size) "cursor-$end" else null,
            hasMore = end < items.size,
        )
    }
}
