package zhaoyun.example.composedemo.story.infobar.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

class InfoBarUseCaseTest {

    private class FakeLikeRepository(
        private val resultProvider: suspend (String, Boolean, Int) -> LikeResult,
    ) : LikeRepository {
        val calls = mutableListOf<Triple<String, Boolean, Int>>()

        override suspend fun toggleLike(cardId: String, isLiked: Boolean, currentLikes: Int): LikeResult {
            calls.add(Triple(cardId, isLiked, currentLikes))
            return resultProvider(cardId, isLiked, currentLikes)
        }
    }

    private fun createUseCase(
        cardId: String = "test-1",
        initialState: InfoBarState = InfoBarState(),
        likeRepository: LikeRepository = FakeLikeRepository { _, isLiked, currentLikes ->
            LikeResult(isLiked = isLiked, likes = if (isLiked) currentLikes + 1 else (currentLikes - 1).coerceAtLeast(0))
        },
        shareRepository: ShareRepository = FakeShareRepository(),
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
    ) = InfoBarUseCase(
        cardId = cardId,
        likeRepository = likeRepository,
        shareRepository = shareRepository,
        scope = scope,
        stateHolder = initialState.toStateHolder(),
        serviceRegistry = MutableServiceRegistryImpl(),
    )

    @Test
    fun `初始状态同步默认值`() {
        val useCase = createUseCase()
        val state = useCase.state.value
        assertEquals("", state.storyTitle)
        assertEquals("", state.creatorName)
        assertEquals("", state.creatorHandle)
        assertEquals(0, state.likes)
        assertEquals(0, state.shares)
        assertEquals(0, state.comments)
        assertFalse(state.isLiked)
    }

    @Test
    fun `点击点赞切换isLiked并增加likes`() = runTest {
        val useCase = createUseCase()
        useCase.receiveEvent(InfoBarEvent.OnLikeClicked)
        assertTrue(useCase.state.value.isLiked)
        assertEquals(1, useCase.state.value.likes)
    }

    @Test
    fun `再次点击点赞恢复原始状态`() = runTest {
        val useCase = createUseCase(
            initialState = InfoBarState(isLiked = true, likes = 1),
        )
        useCase.receiveEvent(InfoBarEvent.OnLikeClicked)
        assertFalse(useCase.state.value.isLiked)
        assertEquals(0, useCase.state.value.likes)
    }

    @Test
    fun `取消点赞时likes不会低于0`() = runTest {
        val useCase = createUseCase(
            initialState = InfoBarState(isLiked = true, likes = 0),
        )

        useCase.receiveEvent(InfoBarEvent.OnLikeClicked)

        assertFalse(useCase.state.value.isLiked)
        assertEquals(0, useCase.state.value.likes)
    }

    @Test
    fun `点击故事标题发送NavigateToStoryDetail效果`() = runTest {
        val useCase = createUseCase(cardId = "story-1")
        val effectDeferred = async { useCase.effect.first() }
        useCase.receiveEvent(InfoBarEvent.OnStoryTitleClicked)
        assertEquals(
            InfoBarEffect.NavigateToStoryDetail("story-1"),
            effectDeferred.await(),
        )
    }

    @Test
    fun `点击分享成功后发送ShowShareSheet效果且不改变状态`() = runTest {
        val initialState = InfoBarState(likes = 3, shares = 2, comments = 1, isLiked = true)
        val useCase = createUseCase(cardId = "story-1", initialState = initialState)

        val effectDeferred = async { useCase.effect.first() }
        useCase.receiveEvent(InfoBarEvent.OnShareClicked)

        assertEquals(
            InfoBarEffect.ShowShareSheet("story-1", "https://example.com/share/story-1"),
            effectDeferred.await(),
        )
        assertEquals(initialState, useCase.state.value)
    }

    @Test
    fun `点击分享失败后发送ShowToast效果且不改变状态`() = runTest {
        val initialState = InfoBarState(likes = 3, shares = 2, comments = 1, isLiked = true)
        val failingShareRepository = object : ShareRepository {
            override suspend fun getShareLink(cardId: String): String {
                throw RuntimeException("network error")
            }
        }
        val useCase = createUseCase(
            cardId = "story-1",
            initialState = initialState,
            shareRepository = failingShareRepository,
        )

        val baseEffectDeferred = async { useCase.baseEffect.first() }
        useCase.receiveEvent(InfoBarEvent.OnShareClicked)

        assertEquals(
            BaseEffect.ShowToast("网络失败"),
            baseEffectDeferred.await(),
        )
        assertEquals(initialState, useCase.state.value)
    }

    @Test
    fun `点击评论发送NavigateToComments效果且不改变状态`() = runTest {
        val initialState = InfoBarState(likes = 3, shares = 2, comments = 1, isLiked = true)
        val useCase = createUseCase(cardId = "story-1", initialState = initialState)

        val effectDeferred = async { useCase.effect.first() }
        useCase.receiveEvent(InfoBarEvent.OnCommentClicked)

        assertEquals(InfoBarEffect.NavigateToComments("story-1"), effectDeferred.await())
        assertEquals(initialState, useCase.state.value)
    }

    @Test
    fun `点击历史发送ShowHistory效果且不改变状态`() = runTest {
        val initialState = InfoBarState(likes = 3, shares = 2, comments = 1, isLiked = true)
        val useCase = createUseCase(cardId = "story-1", initialState = initialState)

        val effectDeferred = async { useCase.effect.first() }
        useCase.receiveEvent(InfoBarEvent.OnHistoryClicked)

        assertEquals(InfoBarEffect.ShowHistory("story-1"), effectDeferred.await())
        assertEquals(initialState, useCase.state.value)
    }

    @Test
    fun `点赞后异步请求成功，以接口结果回写状态`() = runTest {
        val repository = FakeLikeRepository { _, _, _ ->
            LikeResult(isLiked = true, likes = 100)
        }
        val useCase = createUseCase(
            initialState = InfoBarState(isLiked = false, likes = 5),
            likeRepository = repository,
        )
        useCase.receiveEvent(InfoBarEvent.OnLikeClicked)
        assertTrue(useCase.state.value.isLiked)
        assertEquals(100, useCase.state.value.likes)
        assertEquals(listOf(Triple("test-1", true, 5)), repository.calls)
    }

    @Test
    fun `取消点赞后异步请求成功，以接口结果回写状态`() = runTest {
        val repository = FakeLikeRepository { _, _, _ ->
            LikeResult(isLiked = false, likes = 9)
        }
        val useCase = createUseCase(
            initialState = InfoBarState(isLiked = true, likes = 10),
            likeRepository = repository,
        )
        useCase.receiveEvent(InfoBarEvent.OnLikeClicked)
        assertFalse(useCase.state.value.isLiked)
        assertEquals(9, useCase.state.value.likes)
        assertEquals(listOf(Triple("test-1", false, 10)), repository.calls)
    }

    @Test
    fun `乐观更新与接口结果冲突时，以接口结果为准`() = runTest {
        val repository = FakeLikeRepository { _, _, _ ->
            LikeResult(isLiked = true, likes = 99)
        }
        val useCase = createUseCase(
            initialState = InfoBarState(isLiked = true, likes = 10),
            likeRepository = repository,
        )
        useCase.receiveEvent(InfoBarEvent.OnLikeClicked)
        assertTrue(useCase.state.value.isLiked)
        assertEquals(99, useCase.state.value.likes)
    }

    @Test
    fun `点赞请求失败后回滚乐观更新并发送ShowToast`() = runTest {
        val failingRepository = object : LikeRepository {
            override suspend fun toggleLike(cardId: String, isLiked: Boolean, currentLikes: Int): LikeResult {
                throw RuntimeException("server error")
            }
        }
        val useCase = createUseCase(
            initialState = InfoBarState(isLiked = false, likes = 5),
            likeRepository = failingRepository,
        )

        val baseEffectDeferred = async { useCase.baseEffect.first() }
        useCase.receiveEvent(InfoBarEvent.OnLikeClicked)

        assertEquals(
            BaseEffect.ShowToast("操作失败，请重试"),
            baseEffectDeferred.await(),
        )

        assertFalse(useCase.state.value.isLiked)
        assertEquals(5, useCase.state.value.likes)
    }

    @Test
    fun `快速点击只保留最新请求的结果`() = runTest {
        val repository = FakeLikeRepository { _, isLiked, _ ->
            delay(100)
            LikeResult(isLiked = isLiked, likes = if (isLiked) 100 else 0)
        }
        val useCase = createUseCase(
            initialState = InfoBarState(isLiked = false, likes = 0),
            likeRepository = repository,
            scope = CoroutineScope(SupervisorJob() + this.coroutineContext[kotlinx.coroutines.CoroutineDispatcher]!!),
        )

        // 第一次点击：乐观更新为已点赞
        useCase.receiveEvent(InfoBarEvent.OnLikeClicked)
        assertTrue(useCase.state.value.isLiked)
        assertEquals(1, useCase.state.value.likes)

        // 快速第二次点击：乐观更新回未点赞，同时 cancel 第一次的请求
        useCase.receiveEvent(InfoBarEvent.OnLikeClicked)
        assertFalse(useCase.state.value.isLiked)
        assertEquals(0, useCase.state.value.likes)

        // 推进时间，等待剩余协程完成
        advanceUntilIdle()

        // 第一次请求在 delay 挂起点被成功 cancel，只保留第二次请求的结果
        assertFalse(useCase.state.value.isLiked)
        assertEquals(0, useCase.state.value.likes)
        // 只有第二次请求真正执行到了 Repository（传入的是乐观更新后的 currentLikes=1）
        assertEquals(listOf(Triple("test-1", false, 1)), repository.calls)
    }
}
