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
    fun `input change updates text and clears input error`() = runTest {
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                inputText = "旧输入",
                inputErrorMessage = "请输入评论内容",
                sendErrorMessage = "发送失败，请重试",
            ),
        )

        useCase.receiveEvent(CommentPanelEvent.OnInputChanged("新输入"))

        assertEquals("新输入", useCase.state.value.inputText)
        assertEquals(null, useCase.state.value.inputErrorMessage)
        assertEquals(null, useCase.state.value.sendErrorMessage)
    }

    @Test
    fun `blank comment does not send and emits validation toast`() = runTest {
        val repository = RecordingSendRepository()
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                inputText = "  \n\t  ",
            ),
            repository = repository,
        )
        val toastDeferred = async { useCase.baseEffect.first() }

        useCase.receiveEvent(CommentPanelEvent.OnSendClicked)

        assertEquals(0, repository.callCount)
        assertFalse(useCase.state.value.isSendingComment)
        assertEquals("请输入评论内容", useCase.state.value.inputErrorMessage)
        assertEquals(BaseEffect.ShowToast("请输入评论内容"), toastDeferred.await())
    }

    @Test
    fun `overlong comment does not send and emits validation toast`() = runTest {
        val repository = RecordingSendRepository()
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                inputText = "a".repeat(CommentPanelMaxInputLength + 1),
            ),
            repository = repository,
        )
        val toastDeferred = async { useCase.baseEffect.first() }

        useCase.receiveEvent(CommentPanelEvent.OnSendClicked)

        assertEquals(0, repository.callCount)
        assertFalse(useCase.state.value.isSendingComment)
        assertEquals("评论不能超过200字", useCase.state.value.inputErrorMessage)
        assertEquals(BaseEffect.ShowToast("评论不能超过200字"), toastDeferred.await())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `send success prepends comment clears input and updates total count`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val existing = sampleComment("existing")
        val sentComment = commentData("sent-comment", content = "新评论")
        val repository = SuspendedSendRepository(
            result = SendCommentResult(
                comment = sentComment,
                totalCount = 2,
            ),
        )
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                totalCount = 1,
                comments = listOf(existing),
                inputText = "  新评论  ",
                inputErrorMessage = "旧输入错误",
                sendErrorMessage = "旧发送错误",
            ),
            repository = repository,
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )

        useCase.receiveEvent(CommentPanelEvent.OnSendClicked)

        assertTrue(useCase.state.value.isSendingComment)
        assertEquals(null, useCase.state.value.sendErrorMessage)

        runCurrent()
        assertEquals(1, repository.callCount)
        assertEquals("story-1", repository.cardId)
        assertEquals("新评论", repository.content)

        useCase.receiveEvent(CommentPanelEvent.OnSendClicked)
        runCurrent()
        assertEquals(1, repository.callCount)

        repository.complete()
        advanceUntilIdle()

        assertEquals(false, useCase.state.value.isSendingComment)
        assertEquals("", useCase.state.value.inputText)
        assertEquals(null, useCase.state.value.inputErrorMessage)
        assertEquals(null, useCase.state.value.sendErrorMessage)
        assertEquals(2, useCase.state.value.totalCount)
        assertEquals(listOf(sentComment.toCommentItem(), existing), useCase.state.value.comments)
    }

    @Test
    fun `send failure keeps input and does not add failed comment`() = runTest {
        val existing = sampleComment("existing")
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                totalCount = 1,
                comments = listOf(existing),
                inputText = "  保留内容  ",
            ),
            repository = FailingCommentRepository(failSend = true),
        )
        val toastDeferred = async { useCase.baseEffect.first() }

        useCase.receiveEvent(CommentPanelEvent.OnSendClicked)

        assertFalse(useCase.state.value.isSendingComment)
        assertEquals("  保留内容  ", useCase.state.value.inputText)
        assertEquals(listOf(existing), useCase.state.value.comments)
        assertEquals(1, useCase.state.value.totalCount)
        assertEquals("发送失败，请重试", useCase.state.value.sendErrorMessage)
        assertEquals(BaseEffect.ShowToast("发送失败，请重试"), toastDeferred.await())
    }

    @Test
    fun `send cancellation exits sending without toast`() = runTest {
        val existing = sampleComment("existing")
        val repository = CancellingSendRepository()
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                totalCount = 1,
                comments = listOf(existing),
                inputText = "  取消发送  ",
            ),
            repository = repository,
        )

        useCase.receiveEvent(CommentPanelEvent.OnSendClicked)

        assertEquals(1, repository.callCount)
        assertFalse(useCase.state.value.isSendingComment)
        assertEquals("  取消发送  ", useCase.state.value.inputText)
        assertEquals(listOf(existing), useCase.state.value.comments)
        assertEquals(null, useCase.state.value.sendErrorMessage)
        assertEquals(null, withTimeoutOrNull(1) { useCase.baseEffect.first() })
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

    @Test
    fun `expand comment only changes target comment`() = runTest {
        val first = sampleComment("comment-1", isExpanded = false)
        val target = sampleComment("comment-2", isExpanded = false)
        val last = sampleComment("comment-3", isExpanded = true)
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = listOf(first, target, last),
                initialLoadStatus = LoadStatus.Success,
            ),
        )

        useCase.receiveEvent(CommentPanelEvent.OnCommentExpanded("comment-2"))

        assertEquals(first, useCase.state.value.comments[0])
        assertEquals(target.copy(isExpanded = true), useCase.state.value.comments[1])
        assertEquals(last, useCase.state.value.comments[2])
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `like comment uses optimistic update and server result`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = SuspendedLikeRepository(
            result = CommentLikeResult("comment-1", isLiked = true, likeCount = 42),
        )
        val other = sampleComment("comment-2", likeCount = 8, isLiked = false)
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = listOf(
                    sampleComment("comment-1", likeCount = 3, isLiked = false),
                    other,
                ),
                initialLoadStatus = LoadStatus.Success,
            ),
            repository = repository,
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )

        useCase.receiveEvent(CommentPanelEvent.OnCommentLikeClicked("comment-1"))

        assertEquals(
            sampleComment("comment-1", likeCount = 4, isLiked = true, isLikeSubmitting = true),
            useCase.state.value.comments[0],
        )
        assertEquals(other, useCase.state.value.comments[1])

        useCase.receiveEvent(CommentPanelEvent.OnCommentLikeClicked("comment-1"))
        runCurrent()

        assertEquals(1, repository.callCount)
        assertEquals("story-1", repository.cardId)
        assertEquals("comment-1", repository.commentId)
        assertEquals(true, repository.liked)

        repository.complete()
        advanceUntilIdle()

        assertEquals(
            sampleComment("comment-1", likeCount = 42, isLiked = true, isLikeSubmitting = false),
            useCase.state.value.comments[0],
        )
        assertEquals(other, useCase.state.value.comments[1])
    }

    @Test
    fun `unlike comment never produces negative like count`() = runTest {
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = listOf(sampleComment("comment-1", likeCount = 0, isLiked = true)),
                initialLoadStatus = LoadStatus.Success,
            ),
            repository = FixedLikeRepository(CommentLikeResult("comment-1", isLiked = false, likeCount = -1)),
        )

        useCase.receiveEvent(CommentPanelEvent.OnCommentLikeClicked("comment-1"))

        assertEquals(
            sampleComment("comment-1", likeCount = 0, isLiked = false, isLikeSubmitting = false),
            useCase.state.value.comments.single(),
        )
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `unlike comment uses optimistic submitting state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = SuspendedLikeRepository(
            result = CommentLikeResult("comment-1", isLiked = false, likeCount = 1),
        )
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = listOf(sampleComment("comment-1", likeCount = 2, isLiked = true)),
                initialLoadStatus = LoadStatus.Success,
            ),
            repository = repository,
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )

        useCase.receiveEvent(CommentPanelEvent.OnCommentLikeClicked("comment-1"))

        assertEquals(
            sampleComment("comment-1", likeCount = 1, isLiked = false, isLikeSubmitting = true),
            useCase.state.value.comments.single(),
        )
        runCurrent()
        assertEquals(false, repository.liked)
        repository.complete()
        advanceUntilIdle()

        assertEquals(
            sampleComment("comment-1", likeCount = 1, isLiked = false, isLikeSubmitting = false),
            useCase.state.value.comments.single(),
        )
    }

    @Test
    fun `like missing comment does nothing`() = runTest {
        val existing = sampleComment("comment-1", likeCount = 2, isLiked = false)
        val repository = SuspendedLikeRepository(
            result = CommentLikeResult("missing", isLiked = true, likeCount = 3),
        )
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = listOf(existing),
                initialLoadStatus = LoadStatus.Success,
            ),
            repository = repository,
        )

        useCase.receiveEvent(CommentPanelEvent.OnCommentLikeClicked("missing"))

        assertEquals(listOf(existing), useCase.state.value.comments)
        assertEquals(0, repository.callCount)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `expand replies loads first reply page for target comment`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val repository = SuspendedRepliesRepository(
            result = ReplyPage(
                replies = listOf(replyData("reply-1", parentId = "comment-1")),
                nextCursor = "cursor-1",
                hasMore = true,
            ),
        )
        val target = sampleComment("comment-1")
        val other = sampleComment("comment-2")
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = listOf(target, other),
                initialLoadStatus = LoadStatus.Success,
            ),
            repository = repository,
            scope = CoroutineScope(SupervisorJob() + dispatcher),
        )

        useCase.receiveEvent(CommentPanelEvent.OnRepliesExpanded("comment-1"))

        assertEquals(
            target.copy(
                replySection = ReplySectionState(
                    isExpanded = true,
                    isLoading = true,
                ),
            ),
            useCase.state.value.comments[0],
        )
        assertEquals(other, useCase.state.value.comments[1])

        runCurrent()
        assertEquals(1, repository.callCount)
        assertEquals("story-1", repository.cardId)
        assertEquals("comment-1", repository.commentId)
        assertEquals(null, repository.cursor)
        assertEquals(CommentPanelReplyPageSize, repository.pageSize)

        repository.complete()
        advanceUntilIdle()

        assertEquals(
            target.copy(
                replySection = ReplySectionState(
                    isExpanded = true,
                    replies = listOf(sampleReply("reply-1", parentId = "comment-1")),
                    pagination = PaginationState(nextCursor = "cursor-1", hasMore = true),
                ),
            ),
            useCase.state.value.comments[0],
        )
        assertEquals(other, useCase.state.value.comments[1])

        useCase.receiveEvent(CommentPanelEvent.OnRepliesExpanded("comment-1"))

        assertEquals(1, repository.callCount)
    }

    @Test
    fun `reply load failure only marks target comment reply section`() = runTest {
        val loadedReply = sampleReply("reply-1", parentId = "comment-1")
        val target = sampleComment(
            "comment-1",
            replySection = ReplySectionState(
                isExpanded = true,
                replies = listOf(loadedReply),
                pagination = PaginationState(nextCursor = "cursor-1", hasMore = true),
            ),
        )
        val other = sampleComment(
            "comment-2",
            replySection = ReplySectionState(
                isExpanded = true,
                replies = listOf(sampleReply("reply-other", parentId = "comment-2")),
                pagination = PaginationState(nextCursor = "other-cursor", hasMore = true),
            ),
        )
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = listOf(target, other),
                initialLoadStatus = LoadStatus.Success,
            ),
            repository = FailingCommentRepository(failReplies = true),
        )

        useCase.receiveEvent(CommentPanelEvent.OnLoadMoreReplies("comment-1"))

        assertEquals(
            target.copy(
                replySection = target.replySection.copy(
                    isLoading = false,
                    errorMessage = "回复加载失败",
                ),
            ),
            useCase.state.value.comments[0],
        )
        assertEquals(other, useCase.state.value.comments[1])
    }

    @Test
    fun `load more replies appends and deduplicates target replies`() = runTest {
        val existingReply = sampleReply("reply-1", parentId = "comment-1")
        val target = sampleComment(
            "comment-1",
            replySection = ReplySectionState(
                isExpanded = true,
                replies = listOf(existingReply),
                pagination = PaginationState(nextCursor = "cursor-1", hasMore = true),
            ),
        )
        val other = sampleComment("comment-2")
        val repository = FixedRepliesRepository(
            result = ReplyPage(
                replies = listOf(
                    replyData("reply-1", parentId = "comment-1"),
                    replyData("reply-2", parentId = "comment-1"),
                ),
                nextCursor = null,
                hasMore = false,
            ),
        )
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = listOf(target, other),
                initialLoadStatus = LoadStatus.Success,
            ),
            repository = repository,
        )

        useCase.receiveEvent(CommentPanelEvent.OnLoadMoreReplies("comment-1"))

        assertEquals("story-1", repository.cardId)
        assertEquals("comment-1", repository.commentId)
        assertEquals("cursor-1", repository.cursor)
        assertEquals(CommentPanelReplyPageSize, repository.pageSize)
        assertEquals(
            target.copy(
                replySection = ReplySectionState(
                    isExpanded = true,
                    replies = listOf(
                        existingReply,
                        sampleReply("reply-2", parentId = "comment-1"),
                    ),
                    pagination = PaginationState(nextCursor = null, hasMore = false),
                ),
            ),
            useCase.state.value.comments[0],
        )
        assertEquals(other, useCase.state.value.comments[1])
    }

    @Test
    fun `collapse replies keeps loaded replies`() = runTest {
        val target = sampleComment(
            "comment-1",
            replySection = ReplySectionState(
                isExpanded = true,
                replies = listOf(sampleReply("reply-1", parentId = "comment-1")),
                pagination = PaginationState(nextCursor = "cursor-1", hasMore = true),
            ),
        )
        val other = sampleComment(
            "comment-2",
            replySection = ReplySectionState(
                isExpanded = true,
                replies = listOf(sampleReply("reply-other", parentId = "comment-2")),
            ),
        )
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = listOf(target, other),
                initialLoadStatus = LoadStatus.Success,
            ),
        )

        useCase.receiveEvent(CommentPanelEvent.OnRepliesCollapsed("comment-1"))

        assertEquals(
            target.copy(replySection = target.replySection.copy(isExpanded = false)),
            useCase.state.value.comments[0],
        )
        assertEquals(other, useCase.state.value.comments[1])
    }

    @Test
    fun `reply cancellation clears loading without error`() = runTest {
        val existingReply = sampleReply("reply-1", parentId = "comment-1")
        val repository = CancellingRepliesRepository()
        val target = sampleComment(
            "comment-1",
            replySection = ReplySectionState(
                isExpanded = true,
                replies = listOf(existingReply),
                pagination = PaginationState(nextCursor = "cursor-1", hasMore = true),
            ),
        )
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = listOf(target),
                initialLoadStatus = LoadStatus.Success,
            ),
            repository = repository,
        )

        useCase.receiveEvent(CommentPanelEvent.OnLoadMoreReplies("comment-1"))

        assertEquals(1, repository.callCount)
        assertEquals(
            target.copy(
                replySection = target.replySection.copy(
                    isLoading = false,
                    errorMessage = null,
                ),
            ),
            useCase.state.value.comments.single(),
        )
    }

    @Test
    fun `reply loading guards leave comments unchanged`() = runTest {
        val loading = sampleComment(
            "comment-1",
            replySection = ReplySectionState(isExpanded = true, isLoading = true),
        )
        val loadingMore = sampleComment(
            "comment-5",
            replySection = ReplySectionState(
                isExpanded = true,
                isLoading = true,
                pagination = PaginationState(nextCursor = "cursor-5", hasMore = true),
            ),
        )
        val collapsed = sampleComment(
            "comment-2",
            replySection = ReplySectionState(
                isExpanded = false,
                pagination = PaginationState(nextCursor = "cursor-1", hasMore = true),
            ),
        )
        val noMore = sampleComment(
            "comment-3",
            replySection = ReplySectionState(
                isExpanded = true,
                pagination = PaginationState(nextCursor = "cursor-2", hasMore = false),
            ),
        )
        val noCursor = sampleComment(
            "comment-4",
            replySection = ReplySectionState(
                isExpanded = true,
                pagination = PaginationState(nextCursor = null, hasMore = true),
            ),
        )
        val comments = listOf(loading, loadingMore, collapsed, noMore, noCursor)
        val repository = FixedRepliesRepository(
            result = ReplyPage(
                replies = listOf(replyData("reply-guard")),
                nextCursor = null,
                hasMore = false,
            ),
        )
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = comments,
                initialLoadStatus = LoadStatus.Success,
            ),
            repository = repository,
        )

        useCase.receiveEvent(CommentPanelEvent.OnRepliesExpanded("missing"))
        useCase.receiveEvent(CommentPanelEvent.OnRepliesExpanded("comment-1"))
        useCase.receiveEvent(CommentPanelEvent.OnLoadMoreReplies("missing"))
        useCase.receiveEvent(CommentPanelEvent.OnLoadMoreReplies("comment-5"))
        useCase.receiveEvent(CommentPanelEvent.OnLoadMoreReplies("comment-2"))
        useCase.receiveEvent(CommentPanelEvent.OnLoadMoreReplies("comment-3"))
        useCase.receiveEvent(CommentPanelEvent.OnLoadMoreReplies("comment-4"))

        assertEquals(comments, useCase.state.value.comments)
        assertEquals(0, repository.callCount)
    }

    @Test
    fun `like failure rolls back target comment and emits toast`() = runTest {
        val oldTarget = sampleComment("comment-1", likeCount = 7, isLiked = false)
        val other = sampleComment("comment-2", likeCount = 2, isLiked = true)
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = listOf(oldTarget, other),
                initialLoadStatus = LoadStatus.Success,
            ),
            repository = FailingCommentRepository(failLike = true),
        )
        val toastDeferred = async { useCase.baseEffect.first() }

        useCase.receiveEvent(CommentPanelEvent.OnCommentLikeClicked("comment-1"))

        assertEquals(oldTarget, useCase.state.value.comments[0])
        assertEquals(other, useCase.state.value.comments[1])
        assertEquals(BaseEffect.ShowToast("点赞失败，请重试"), toastDeferred.await())
    }

    @Test
    fun `like cancellation rolls back without toast`() = runTest {
        val oldTarget = sampleComment("comment-1", likeCount = 7, isLiked = false)
        val useCase = createUseCase(
            initialState = CommentPanelState(
                cardId = "story-1",
                comments = listOf(oldTarget),
                initialLoadStatus = LoadStatus.Success,
            ),
            repository = CancellingLikeRepository(),
        )

        useCase.receiveEvent(CommentPanelEvent.OnCommentLikeClicked("comment-1"))

        assertEquals(listOf(oldTarget), useCase.state.value.comments)
        assertEquals(null, withTimeoutOrNull(1) { useCase.baseEffect.first() })
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

private fun sampleComment(
    id: String,
    likeCount: Int = 0,
    isLiked: Boolean = false,
    isLikeSubmitting: Boolean = false,
    isExpanded: Boolean = false,
    replySection: ReplySectionState = ReplySectionState(),
) = CommentItem(
    commentId = id,
    user = sampleUser(),
    content = "测试评论",
    createdAtText = "刚刚",
    likeCount = likeCount,
    isLiked = isLiked,
    isLikeSubmitting = isLikeSubmitting,
    isExpanded = isExpanded,
    replySection = replySection,
)

private fun sampleReply(id: String, parentId: String = "comment-1") = ReplyItem(
    replyId = id,
    parentCommentId = parentId,
    user = sampleUser("reply-user"),
    content = "测试回复",
    createdAtText = "刚刚",
)

private fun replyData(id: String, parentId: String = "comment-1") = ReplyData(
    replyId = id,
    parentCommentId = parentId,
    user = sampleUser("reply-user"),
    content = "测试回复",
    createdAtText = "刚刚",
)

private fun commentData(id: String, content: String = "测试评论") = CommentData(
    commentId = id,
    user = sampleUser(),
    content = content,
    createdAtText = "刚刚",
    likeCount = 0,
    isLiked = false,
    isPinned = false,
    canExpand = false,
    replyCount = 0,
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

private class FixedLikeRepository(
    private val result: CommentLikeResult,
) : CommentRepository by FakeCommentRepository() {
    override suspend fun setCommentLiked(cardId: String, commentId: String, liked: Boolean): CommentLikeResult {
        return result
    }
}

private class FixedRepliesRepository(
    private val result: ReplyPage,
) : CommentRepository by FakeCommentRepository() {
    var cardId: String? = null
        private set
    var commentId: String? = null
        private set
    var cursor: String? = null
        private set
    var pageSize: Int? = null
        private set
    var callCount: Int = 0
        private set

    override suspend fun loadReplies(cardId: String, commentId: String, cursor: String?, pageSize: Int): ReplyPage {
        callCount += 1
        this.cardId = cardId
        this.commentId = commentId
        this.cursor = cursor
        this.pageSize = pageSize
        return result
    }
}

private class RecordingSendRepository : CommentRepository by FakeCommentRepository() {
    var cardId: String? = null
        private set
    var content: String? = null
        private set
    var callCount: Int = 0
        private set

    override suspend fun sendComment(cardId: String, content: String): SendCommentResult {
        callCount += 1
        this.cardId = cardId
        this.content = content
        return SendCommentResult(commentData("sent-comment", content), totalCount = 1)
    }
}

private class SuspendedSendRepository(
    private val result: SendCommentResult,
) : CommentRepository by FakeCommentRepository() {
    private val canComplete = CompletableDeferred<Unit>()

    var cardId: String? = null
        private set
    var content: String? = null
        private set
    var callCount: Int = 0
        private set

    override suspend fun sendComment(cardId: String, content: String): SendCommentResult {
        callCount += 1
        this.cardId = cardId
        this.content = content
        canComplete.await()
        return result
    }

    fun complete() {
        canComplete.complete(Unit)
    }
}

private class CancellingSendRepository : CommentRepository by FakeCommentRepository() {
    var callCount: Int = 0
        private set

    override suspend fun sendComment(cardId: String, content: String): SendCommentResult {
        callCount += 1
        throw CancellationException("send cancelled")
    }
}

private class SuspendedLikeRepository(
    private val result: CommentLikeResult,
) : CommentRepository by FakeCommentRepository() {
    private val canComplete = CompletableDeferred<Unit>()

    var cardId: String? = null
        private set
    var commentId: String? = null
        private set
    var liked: Boolean? = null
        private set
    var callCount: Int = 0
        private set

    override suspend fun setCommentLiked(cardId: String, commentId: String, liked: Boolean): CommentLikeResult {
        callCount += 1
        this.cardId = cardId
        this.commentId = commentId
        this.liked = liked
        canComplete.await()
        return result
    }

    fun complete() {
        canComplete.complete(Unit)
    }
}

private class SuspendedRepliesRepository(
    private val result: ReplyPage,
) : CommentRepository by FakeCommentRepository() {
    private val canComplete = CompletableDeferred<Unit>()

    var cardId: String? = null
        private set
    var commentId: String? = null
        private set
    var cursor: String? = null
        private set
    var pageSize: Int? = null
        private set
    var callCount: Int = 0
        private set

    override suspend fun loadReplies(cardId: String, commentId: String, cursor: String?, pageSize: Int): ReplyPage {
        callCount += 1
        this.cardId = cardId
        this.commentId = commentId
        this.cursor = cursor
        this.pageSize = pageSize
        canComplete.await()
        return result
    }

    fun complete() {
        canComplete.complete(Unit)
    }
}

private class CancellingLikeRepository : CommentRepository by FakeCommentRepository() {
    override suspend fun setCommentLiked(cardId: String, commentId: String, liked: Boolean): CommentLikeResult {
        throw CancellationException("like cancelled")
    }
}

private class CancellingRepliesRepository : CommentRepository by FakeCommentRepository() {
    var callCount: Int = 0
        private set

    override suspend fun loadReplies(cardId: String, commentId: String, cursor: String?, pageSize: Int): ReplyPage {
        callCount += 1
        throw CancellationException("replies cancelled")
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
