package zhaoyun.example.composedemo.feed.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import zhaoyun.example.composedemo.service.feed.mock.FakeFeedRepository

class FeedUseCaseTest {

    private val fakeRepository = FakeFeedRepository()
    private lateinit var useCase: FeedUseCase

    @Before
    fun setup() {
        useCase = FeedUseCase(fakeRepository)
    }

    @Test
    fun `初始状态为空列表且不加载`() {
        val state = useCase.state.value
        assertTrue(state.cards.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
    }

    @Test
    fun `刷新事件触发刷新状态`() = runTest {
        useCase.receiveEvent(FeedEvent.OnRefresh)
        assertFalse(useCase.state.value.isRefreshing)
    }

    @Test
    fun `刷新成功填充数据`() = runTest {
        useCase.receiveEvent(FeedEvent.OnRefresh)
        val state = useCase.state.value
        assertEquals(10, state.cards.size)
        assertFalse(state.isRefreshing)
        assertEquals(1, state.currentPage)
    }

    @Test
    fun `加载更多追加数据`() = runTest {
        useCase.receiveEvent(FeedEvent.OnRefresh)
        useCase.receiveEvent(FeedEvent.OnLoadMore)
        val state = useCase.state.value
        assertEquals(15, state.cards.size)
        assertTrue(state.hasMore)
    }

    @Test
    fun `加载更多到第三页hasMore为false`() = runTest {
        useCase.receiveEvent(FeedEvent.OnRefresh)      // page 0 -> 10条
        useCase.receiveEvent(FeedEvent.OnLoadMore)     // page 1 -> +5条
        useCase.receiveEvent(FeedEvent.OnLoadMore)     // page 2 -> 空
        val state = useCase.state.value
        assertEquals(15, state.cards.size)
        assertFalse(state.hasMore)
    }
}
