package zhaoyun.example.composedemo.story.commentpanel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentPanelUseCaseTest {
    @Test
    fun `初始状态表达可加载的空评论面板`() {
        val state = CommentPanelState(cardId = "story-1")

        assertEquals("story-1", state.cardId)
        assertEquals(0, state.totalCount)
        assertEquals(LoadStatus.Idle, state.initialLoadStatus)
        assertEquals(DialogueEntryState.Hidden, state.dialogueEntry)
        assertEquals(emptyList<CommentItem>(), state.comments)
        assertFalse(state.commentPagination.hasMore)
        assertEquals("", state.inputText)
        assertFalse(state.isSendingComment)
        assertEquals(null, state.inputErrorMessage)
        assertEquals(null, state.sendErrorMessage)
    }

    @Test
    fun `评论模型包含用户点赞展开和回复状态`() {
        val user = CommentUser(
            userId = "user-1",
            nickname = "小云",
            avatarUrl = "https://example.com/u.png",
            isAuthor = true,
        )
        val comment = CommentItem(
            commentId = "comment-1",
            user = user,
            content = "这是一条评论",
            createdAtText = "刚刚",
            likeCount = 3,
            isLiked = false,
            isPinned = true,
            canExpand = true,
            replyCount = 2,
        )

        assertEquals("comment-1", comment.commentId)
        assertEquals(user, comment.user)
        assertFalse(comment.isLikeSubmitting)
        assertFalse(comment.isExpanded)
        assertFalse(comment.replySection.isExpanded)
        assertEquals(emptyList<ReplyItem>(), comment.replySection.replies)
        assertTrue(comment.canExpand)
    }

    @Test
    fun `仓库数据转换为面板模型时重置本地瞬态状态`() {
        val user = CommentUser(
            userId = "user-1",
            nickname = "小云",
            avatarUrl = "https://example.com/u.png",
            isAuthor = true,
        )
        val commentData = CommentData(
            commentId = "comment-1",
            user = user,
            content = "这是一条评论",
            createdAtText = "刚刚",
            likeCount = 3,
            isLiked = true,
            isPinned = true,
            canExpand = true,
            replyCount = 2,
        )
        val replyData = ReplyData(
            replyId = "reply-1",
            parentCommentId = "comment-1",
            user = user,
            content = "这是一条回复",
            createdAtText = "1分钟前",
        )

        val comment = commentData.toCommentItem()
        val reply = replyData.toReplyItem()

        assertEquals("comment-1", comment.commentId)
        assertEquals(user, comment.user)
        assertEquals("这是一条评论", comment.content)
        assertEquals("刚刚", comment.createdAtText)
        assertEquals(3, comment.likeCount)
        assertTrue(comment.isLiked)
        assertTrue(comment.isPinned)
        assertTrue(comment.canExpand)
        assertEquals(2, comment.replyCount)
        assertFalse(comment.isLikeSubmitting)
        assertFalse(comment.isExpanded)
        assertEquals(ReplySectionState(), comment.replySection)
        assertEquals(
            ReplyItem(
                replyId = "reply-1",
                parentCommentId = "comment-1",
                user = user,
                content = "这是一条回复",
                createdAtText = "1分钟前",
            ),
            reply,
        )
    }

    @Test
    fun `fake repository returns dialogue entry and first comment page`() = kotlinx.coroutines.test.runTest {
        val repository = FakeCommentRepository()

        val result = repository.loadInitial(cardId = "story-1", pageSize = 2)

        assertEquals(5, result.totalCount)
        assertTrue(result.dialogueEntry is DialogueEntryState.Available)
        assertEquals(2, result.page.comments.size)
        assertEquals("comment-1", result.page.comments.first().commentId)
        assertEquals("cursor-2", result.page.nextCursor)
        assertTrue(result.page.hasMore)
    }

    @Test
    fun `fake repository supports comments replies like and send`() = kotlinx.coroutines.test.runTest {
        val repository = FakeCommentRepository()

        val moreComments = repository.loadMoreComments(cardId = "story-1", cursor = "cursor-2", pageSize = 2)
        val replies = repository.loadReplies(cardId = "story-1", commentId = "comment-1", cursor = null, pageSize = 1)
        val like = repository.setCommentLiked(cardId = "story-1", commentId = "comment-1", liked = true)
        val send = repository.sendComment(cardId = "story-1", content = "新评论")

        assertEquals(listOf("comment-3", "comment-4"), moreComments.comments.map { it.commentId })
        assertEquals(1, replies.replies.size)
        assertEquals("reply-1", replies.replies.first().replyId)
        assertEquals(CommentLikeResult("comment-1", isLiked = true, likeCount = 13), like)
        assertEquals("新评论", send.comment.content)
        assertEquals(6, send.totalCount)
    }
}
