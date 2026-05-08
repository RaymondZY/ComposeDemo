package zhaoyun.example.composedemo.story.commentpanel.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

class CommentPanelUseCaseTest {

    private fun createUseCase(
        cardId: String = "story-1",
        commentRepository: CommentRepository = FakeCommentRepository(),
    ) = CommentPanelUseCase(
        cardId = cardId,
        commentRepository = commentRepository,
        scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
        stateHolder = CommentPanelState(cardId = cardId).toStateHolder(),
        serviceRegistry = MutableServiceRegistryImpl(),
    )

    @Test
    fun `打开面板成功加载评论列表`() = runTest {
        val useCase = createUseCase(cardId = "story-1")

        useCase.receiveEvent(CommentPanelEvent.OnPanelShown)

        assertEquals("story-1", useCase.state.value.cardId)
        assertEquals(false, useCase.state.value.isLoadingComments)
        assertEquals(
            listOf(
                CommentItem("comment-1", "小云", "这个故事很有意思"),
                CommentItem("comment-2", "访客", "期待后续"),
            ),
            useCase.state.value.comments,
        )
    }

    @Test
    fun `打开面板加载评论失败时提示且结束加载`() = runTest {
        val repository = object : CommentRepository {
            override suspend fun loadComments(cardId: String): List<CommentItem> {
                throw RuntimeException("network error")
            }

            override suspend fun sendComment(cardId: String, content: String): CommentItem {
                error("not used")
            }
        }
        val useCase = createUseCase(commentRepository = repository)

        val baseEffectDeferred = async { useCase.baseEffect.first() }
        useCase.receiveEvent(CommentPanelEvent.OnPanelShown)

        assertEquals(BaseEffect.ShowToast("评论加载失败"), baseEffectDeferred.await())
        assertEquals(emptyList<CommentItem>(), useCase.state.value.comments)
        assertEquals(false, useCase.state.value.isLoadingComments)
    }

    @Test
    fun `评论列表为空时状态稳定`() = runTest {
        val repository = object : CommentRepository {
            override suspend fun loadComments(cardId: String): List<CommentItem> = emptyList()

            override suspend fun sendComment(cardId: String, content: String): CommentItem {
                error("not used")
            }
        }
        val useCase = createUseCase(commentRepository = repository)

        useCase.receiveEvent(CommentPanelEvent.OnPanelShown)

        assertEquals(emptyList<CommentItem>(), useCase.state.value.comments)
        assertEquals(false, useCase.state.value.isLoadingComments)
    }

    @Test
    fun `输入评论内容会更新状态`() = runTest {
        val useCase = createUseCase()

        useCase.receiveEvent(CommentPanelEvent.OnInputChanged("hello"))

        assertEquals("hello", useCase.state.value.inputText)
    }

    @Test
    fun `发送有效评论成功后新增评论并清空输入`() = runTest {
        val useCase = createUseCase()
        useCase.receiveEvent(CommentPanelEvent.OnInputChanged("新评论"))

        useCase.receiveEvent(CommentPanelEvent.OnSendClicked)

        assertEquals("", useCase.state.value.inputText)
        assertEquals(false, useCase.state.value.isSendingComment)
        assertEquals(CommentItem("local-story-1-新评论", "我", "新评论"), useCase.state.value.comments.last())
    }

    @Test
    fun `发送空评论时提示且不提交`() = runTest {
        val repository = CountingCommentRepository()
        val useCase = createUseCase(commentRepository = repository)

        val baseEffectDeferred = async { useCase.baseEffect.first() }
        useCase.receiveEvent(CommentPanelEvent.OnInputChanged("   "))
        useCase.receiveEvent(CommentPanelEvent.OnSendClicked)

        assertEquals(BaseEffect.ShowToast("请输入评论内容"), baseEffectDeferred.await())
        assertEquals(0, repository.sendCalls)
    }

    @Test
    fun `发送评论失败时提示并保留输入`() = runTest {
        val repository = object : CommentRepository {
            override suspend fun loadComments(cardId: String): List<CommentItem> = emptyList()

            override suspend fun sendComment(cardId: String, content: String): CommentItem {
                throw RuntimeException("server error")
            }
        }
        val useCase = createUseCase(commentRepository = repository)

        val baseEffectDeferred = async { useCase.baseEffect.first() }
        useCase.receiveEvent(CommentPanelEvent.OnInputChanged("失败评论"))
        useCase.receiveEvent(CommentPanelEvent.OnSendClicked)

        assertEquals(BaseEffect.ShowToast("评论发送失败"), baseEffectDeferred.await())
        assertEquals("失败评论", useCase.state.value.inputText)
        assertEquals(emptyList<CommentItem>(), useCase.state.value.comments)
        assertEquals(false, useCase.state.value.isSendingComment)
    }

    @Test
    fun `评论加载中重复打开面板不会重复请求`() = runTest {
        val repository = CountingCommentRepository()
        val useCase = CommentPanelUseCase(
            cardId = "story-1",
            commentRepository = repository,
            scope = CoroutineScope(SupervisorJob() + coroutineContext),
            stateHolder = CommentPanelState(cardId = "story-1").toStateHolder(),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        useCase.receiveEvent(CommentPanelEvent.OnPanelShown)
        useCase.receiveEvent(CommentPanelEvent.OnPanelShown)
        advanceUntilIdle()

        assertEquals(1, repository.loadCalls)
    }

    private class CountingCommentRepository : CommentRepository {
        var loadCalls = 0
        var sendCalls = 0

        override suspend fun loadComments(cardId: String): List<CommentItem> {
            loadCalls += 1
            delay(100)
            return emptyList()
        }

        override suspend fun sendComment(cardId: String, content: String): CommentItem {
            sendCalls += 1
            return CommentItem("new", "我", content)
        }
    }
}
