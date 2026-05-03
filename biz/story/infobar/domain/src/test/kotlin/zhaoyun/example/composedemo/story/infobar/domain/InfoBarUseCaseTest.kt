package zhaoyun.example.composedemo.story.infobar.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

class InfoBarUseCaseTest {

    private class FakeLikeRepository(
        private val resultProvider: suspend (String, Boolean) -> LikeResult,
    ) : LikeRepository {
        val calls = mutableListOf<Pair<String, Boolean>>()

        override suspend fun toggleLike(cardId: String, isLiked: Boolean): LikeResult {
            calls.add(cardId to isLiked)
            return resultProvider(cardId, isLiked)
        }
    }

    private fun createUseCase(
        cardId: String = "test-1",
        initialState: InfoBarState = InfoBarState(),
        repository: LikeRepository = FakeLikeRepository { _, isLiked ->
            LikeResult(isLiked = isLiked, likes = if (isLiked) 1 else 0)
        },
    ) = InfoBarUseCase(
        cardId = cardId,
        likeRepository = repository,
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
    fun `点击作者区域发送NavigateToCreatorProfile效果`() = runTest {
        val useCase = createUseCase(
            initialState = InfoBarState(creatorHandle = "author_123"),
        )
        val effectDeferred = async { useCase.effect.first() }
        useCase.receiveEvent(InfoBarEvent.OnCreatorClicked)
        assertEquals(
            InfoBarEffect.NavigateToCreatorProfile("author_123"),
            effectDeferred.await(),
        )
    }

    @Test
    fun `点赞后异步请求成功，以接口结果回写状态`() = runTest {
        val repository = FakeLikeRepository { _, _ ->
            LikeResult(isLiked = true, likes = 100)
        }
        val useCase = createUseCase(
            initialState = InfoBarState(isLiked = false, likes = 5),
            repository = repository,
        )
        useCase.receiveEvent(InfoBarEvent.OnLikeClicked)
        assertTrue(useCase.state.value.isLiked)
        assertEquals(100, useCase.state.value.likes)
        assertEquals(listOf("test-1" to true), repository.calls)
    }

    @Test
    fun `取消点赞后异步请求成功，以接口结果回写状态`() = runTest {
        val repository = FakeLikeRepository { _, _ ->
            LikeResult(isLiked = false, likes = 9)
        }
        val useCase = createUseCase(
            initialState = InfoBarState(isLiked = true, likes = 10),
            repository = repository,
        )
        useCase.receiveEvent(InfoBarEvent.OnLikeClicked)
        assertFalse(useCase.state.value.isLiked)
        assertEquals(9, useCase.state.value.likes)
        assertEquals(listOf("test-1" to false), repository.calls)
    }

    @Test
    fun `乐观更新与接口结果冲突时，以接口结果为准`() = runTest {
        val repository = FakeLikeRepository { _, _ ->
            LikeResult(isLiked = true, likes = 99)
        }
        val useCase = createUseCase(
            initialState = InfoBarState(isLiked = true, likes = 10),
            repository = repository,
        )
        useCase.receiveEvent(InfoBarEvent.OnLikeClicked)
        assertTrue(useCase.state.value.isLiked)
        assertEquals(99, useCase.state.value.likes)
    }
}
