package zhaoyun.example.composedemo.story.commentpanel.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

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

    @Test
    fun `panel shown loads first page and dialogue entry`() = runTest {
        val useCase = createUseCase(repository = FakeCommentRepository())

        useCase.receiveEvent(CommentPanelEvent.OnPanelShown)

        assertEquals(LoadStatus.Success, useCase.state.value.initialLoadStatus)
        assertEquals(5, useCase.state.value.totalCount)
        assertEquals(
            listOf("comment-1", "comment-2", "comment-3", "comment-4", "comment-5"),
            useCase.state.value.comments.map { it.commentId },
        )
        assertTrue(useCase.state.value.dialogueEntry is DialogueEntryState.Available)
        assertTrue(useCase.state.value.comments.none { it.isExpanded || it.isLikeSubmitting || it.replySection != ReplySectionState() })
    }

    @Test
    fun `initial load empty page enters empty state`() = runTest {
        val useCase = createUseCase(repository = EmptyCommentRepository())

        useCase.receiveEvent(CommentPanelEvent.OnPanelShown)

        assertEquals(LoadStatus.Empty, useCase.state.value.initialLoadStatus)
        assertEquals(emptyList<CommentItem>(), useCase.state.value.comments)
    }

    @Test
    fun `initial load failure keeps existing comments`() = runTest {
        val existing = sampleComment("existing")
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = listOf(existing),
                initialLoadStatus = LoadStatus.Success,
            ),
            repository = FailingCommentRepository(failInitial = true),
        )
        val toastDeferred = async { useCase.baseEffect.first() }

        useCase.receiveEvent(CommentPanelEvent.OnRetryInitialLoad)

        assertEquals(LoadStatus.Error, useCase.state.value.initialLoadStatus)
        assertEquals(listOf(existing), useCase.state.value.comments)
        assertEquals(BaseEffect.ShowToast("评论加载失败"), toastDeferred.await())
    }

    @Test
    fun `dialogue entry click emits navigation when available`() = runTest {
        val useCase = createUseCase(repository = FakeCommentRepository())
        useCase.receiveEvent(CommentPanelEvent.OnPanelShown)
        val effectDeferred = async { useCase.effect.first() }

        useCase.receiveEvent(CommentPanelEvent.OnDialogueEntryClicked)

        assertEquals(CommentPanelEffect.NavigateToDialogue("story-1", "story-1-dialogue"), effectDeferred.await())
    }

    @Test
    fun `load more comments appends and preserves existing comments on failure`() = runTest {
        val repository = PagedThenFailingCommentRepository()
        val useCase = createUseCase(repository = repository)

        useCase.receiveEvent(CommentPanelEvent.OnPanelShown)
        useCase.receiveEvent(CommentPanelEvent.OnLoadMoreComments)
        val beforeFailure = useCase.state.value.comments
        useCase.receiveEvent(CommentPanelEvent.OnLoadMoreComments)

        assertEquals(listOf("comment-1", "comment-2", "comment-3", "comment-4"), beforeFailure.map { it.commentId })
        assertEquals(beforeFailure, useCase.state.value.comments)
        assertEquals("评论加载失败", useCase.state.value.commentPagination.errorMessage)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `retry result is not overwritten by stale initial load response`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = NonCooperativeStaleInitialLoadRepository()
        val useCase = createUseCase(
            repository = repository,
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )

        useCase.receiveEvent(CommentPanelEvent.OnPanelShown)
        runCurrent()
        useCase.receiveEvent(CommentPanelEvent.OnRetryInitialLoad)
        runCurrent()
        repository.releaseFirstLoad()
        advanceUntilIdle()

        assertEquals(LoadStatus.Success, useCase.state.value.initialLoadStatus)
        assertEquals(1, useCase.state.value.totalCount)
        assertEquals(listOf("retry-comment"), useCase.state.value.comments.map { it.commentId })
        assertEquals("retry-dialogue", (useCase.state.value.dialogueEntry as DialogueEntryState.Available).targetId)
    }

    @Test
    fun `load more cancellation clears loading and remains retryable without error`() = runTest {
        val repository = CancellingThenSuccessfulLoadMoreRepository()
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = listOf(sampleComment("existing")),
                initialLoadStatus = LoadStatus.Success,
                commentPagination = PaginationState(nextCursor = "cursor-1", hasMore = true),
            ),
            repository = repository,
        )

        useCase.receiveEvent(CommentPanelEvent.OnLoadMoreComments)
        assertEquals(false, useCase.state.value.commentPagination.isLoading)
        assertEquals(null, useCase.state.value.commentPagination.errorMessage)
        useCase.receiveEvent(CommentPanelEvent.OnLoadMoreComments)

        assertEquals(listOf("existing", "retry-comment"), useCase.state.value.comments.map { it.commentId })
        assertEquals(false, useCase.state.value.commentPagination.isLoading)
        assertEquals(null, useCase.state.value.commentPagination.errorMessage)
    }

    @Test
    fun `initial load cancellation restores non loading state and remains retryable without error`() = runTest {
        val repository = CancellingThenSuccessfulInitialLoadRepository()
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = listOf(sampleComment("existing")),
                initialLoadStatus = LoadStatus.Success,
            ),
            repository = repository,
        )

        useCase.receiveEvent(CommentPanelEvent.OnRetryInitialLoad)
        assertEquals(LoadStatus.Success, useCase.state.value.initialLoadStatus)
        assertEquals(listOf("existing"), useCase.state.value.comments.map { it.commentId })
        assertEquals(null, withTimeoutOrNull(1) { useCase.baseEffect.first() })
        useCase.receiveEvent(CommentPanelEvent.OnRetryInitialLoad)

        assertEquals(LoadStatus.Success, useCase.state.value.initialLoadStatus)
        assertEquals(listOf("retry-initial-comment"), useCase.state.value.comments.map { it.commentId })
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `stale initial load cancellation does not restore old state over retry result`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = StaleCancellingInitialLoadRepository()
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                initialLoadStatus = LoadStatus.Error,
            ),
            repository = repository,
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )

        useCase.receiveEvent(CommentPanelEvent.OnRetryInitialLoad)
        runCurrent()
        useCase.receiveEvent(CommentPanelEvent.OnRetryInitialLoad)
        runCurrent()
        repository.releaseFirstCancellation()
        advanceUntilIdle()

        assertEquals(LoadStatus.Success, useCase.state.value.initialLoadStatus)
        assertEquals(listOf("retry-after-stale-cancel"), useCase.state.value.comments.map { it.commentId })
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
private fun createUseCase(
    initialState: CommentPanelState = CommentPanelState(cardId = "story-1"),
    repository: CommentRepository = FakeCommentRepository(),
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
) = CommentPanelUseCase(
    commentRepository = repository,
    scope = scope,
    stateHolder = initialState.toStateHolder(),
    serviceRegistry = MutableServiceRegistryImpl(),
)

private fun sampleUser(id: String = "user-1") = CommentUser(
    userId = id,
    nickname = "测试用户",
    avatarUrl = "https://example.com/$id.png",
)

private fun sampleComment(id: String, replySection: ReplySectionState = ReplySectionState()) = CommentItem(
    commentId = id,
    user = sampleUser(),
    content = "测试评论",
    createdAtText = "刚刚",
    likeCount = 0,
    replySection = replySection,
)

private fun sampleReply(id: String, parentId: String = "comment-1") = ReplyItem(
    replyId = id,
    parentCommentId = parentId,
    user = sampleUser("reply-user"),
    content = "测试回复",
    createdAtText = "刚刚",
)

private class EmptyCommentRepository : CommentRepository by FakeCommentRepository() {
    override suspend fun loadInitial(cardId: String, pageSize: Int): CommentInitialResult {
        return CommentInitialResult(
            totalCount = 0,
            dialogueEntry = DialogueEntryState.Hidden,
            page = CommentPage(emptyList(), nextCursor = null, hasMore = false),
        )
    }
}

private class FailingCommentRepository(
    private val failInitial: Boolean = false,
    private val failLike: Boolean = false,
    private val failReplies: Boolean = false,
    private val failSend: Boolean = false,
) : CommentRepository by FakeCommentRepository() {
    override suspend fun loadInitial(cardId: String, pageSize: Int): CommentInitialResult {
        if (failInitial) error("initial failed")
        return FakeCommentRepository().loadInitial(cardId, pageSize)
    }

    override suspend fun setCommentLiked(cardId: String, commentId: String, liked: Boolean): CommentLikeResult {
        if (failLike) error("like failed")
        return FakeCommentRepository().setCommentLiked(cardId, commentId, liked)
    }

    override suspend fun loadReplies(cardId: String, commentId: String, cursor: String?, pageSize: Int): ReplyPage {
        if (failReplies) error("replies failed")
        return FakeCommentRepository().loadReplies(cardId, commentId, cursor, pageSize)
    }

    override suspend fun sendComment(cardId: String, content: String): SendCommentResult {
        if (failSend) error("send failed")
        return FakeCommentRepository().sendComment(cardId, content)
    }
}

private class PagedThenFailingCommentRepository : CommentRepository by FakeCommentRepository() {
    private var loadMoreCalls = 0

    override suspend fun loadInitial(cardId: String, pageSize: Int): CommentInitialResult {
        return FakeCommentRepository().loadInitial(cardId, pageSize = 2)
    }

    override suspend fun loadMoreComments(cardId: String, cursor: String, pageSize: Int): CommentPage {
        loadMoreCalls += 1
        if (loadMoreCalls > 1) error("load more failed")
        return FakeCommentRepository().loadMoreComments(cardId, cursor, pageSize = 2)
    }
}

private class NonCooperativeStaleInitialLoadRepository : CommentRepository by FakeCommentRepository() {
    private val firstLoadCanFinish = CompletableDeferred<Unit>()
    private var loadInitialCalls = 0

    override suspend fun loadInitial(cardId: String, pageSize: Int): CommentInitialResult {
        loadInitialCalls += 1
        return if (loadInitialCalls == 1) {
            withContext(NonCancellable) {
                firstLoadCanFinish.await()
            }
            result("stale-comment", totalCount = 9, targetId = "stale-dialogue")
        } else {
            result("retry-comment", totalCount = 1, targetId = "retry-dialogue")
        }
    }

    fun releaseFirstLoad() {
        firstLoadCanFinish.complete(Unit)
    }

    private fun result(commentId: String, totalCount: Int, targetId: String): CommentInitialResult {
        return CommentInitialResult(
            totalCount = totalCount,
            dialogueEntry = DialogueEntryState.Available(
                title = "进入对话剧情",
                description = "和角色继续聊下去",
                targetId = targetId,
            ),
            page = CommentPage(
                comments = listOf(
                    CommentData(
                        commentId = commentId,
                        user = sampleUser(),
                        content = "测试评论",
                        createdAtText = "刚刚",
                        likeCount = 0,
                        isLiked = false,
                        isPinned = false,
                        canExpand = false,
                        replyCount = 0,
                    ),
                ),
                nextCursor = null,
                hasMore = false,
            ),
        )
    }
}

private class CancellingThenSuccessfulLoadMoreRepository : CommentRepository by FakeCommentRepository() {
    private var calls = 0

    override suspend fun loadMoreComments(cardId: String, cursor: String, pageSize: Int): CommentPage {
        calls += 1
        if (calls == 1) throw CancellationException("load more cancelled")
        return CommentPage(
            comments = listOf(
                CommentData(
                    commentId = "retry-comment",
                    user = sampleUser(),
                    content = "重试评论",
                    createdAtText = "刚刚",
                    likeCount = 0,
                    isLiked = false,
                    isPinned = false,
                    canExpand = false,
                    replyCount = 0,
                ),
            ),
            nextCursor = null,
            hasMore = false,
        )
    }
}

private class CancellingThenSuccessfulInitialLoadRepository : CommentRepository by FakeCommentRepository() {
    private var calls = 0

    override suspend fun loadInitial(cardId: String, pageSize: Int): CommentInitialResult {
        calls += 1
        if (calls == 1) throw CancellationException("initial load cancelled")
        return CommentInitialResult(
            totalCount = 1,
            dialogueEntry = DialogueEntryState.Hidden,
            page = CommentPage(
                comments = listOf(
                    CommentData(
                        commentId = "retry-initial-comment",
                        user = sampleUser(),
                        content = "重试首屏评论",
                        createdAtText = "刚刚",
                        likeCount = 0,
                        isLiked = false,
                        isPinned = false,
                        canExpand = false,
                        replyCount = 0,
                    ),
                ),
                nextCursor = null,
                hasMore = false,
            ),
        )
    }
}

private class StaleCancellingInitialLoadRepository : CommentRepository by FakeCommentRepository() {
    private val firstLoadCanCancel = CompletableDeferred<Unit>()
    private var calls = 0

    override suspend fun loadInitial(cardId: String, pageSize: Int): CommentInitialResult {
        calls += 1
        return if (calls == 1) {
            withContext(NonCancellable) {
                firstLoadCanCancel.await()
                throw CancellationException("stale initial load cancelled")
            }
        } else {
            CommentInitialResult(
                totalCount = 1,
                dialogueEntry = DialogueEntryState.Hidden,
                page = CommentPage(
                    comments = listOf(
                        CommentData(
                            commentId = "retry-after-stale-cancel",
                            user = sampleUser(),
                            content = "重试首屏评论",
                            createdAtText = "刚刚",
                            likeCount = 0,
                            isLiked = false,
                            isPinned = false,
                            canExpand = false,
                            replyCount = 0,
                        ),
                    ),
                    nextCursor = null,
                    hasMore = false,
                ),
            )
        }
    }

    fun releaseFirstCancellation() {
        firstLoadCanCancel.complete(Unit)
    }
}
