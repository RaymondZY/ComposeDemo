package zhaoyun.example.composedemo.feed.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.service.feed.api.FeedRepository
import zhaoyun.example.composedemo.service.feed.api.model.FeedCard
import zhaoyun.example.composedemo.service.feed.mock.FakeFeedRepository

@OptIn(ExperimentalCoroutinesApi::class)
class FeedUseCaseTest {

    private val fakeRepository = FakeFeedRepository()
    private lateinit var useCase: FeedUseCase

    @Before
    fun setup() {
        useCase = FeedUseCase(fakeRepository, FeedState().toStateHolder(), MutableServiceRegistryImpl())
    }

    @Test
    fun `初始状态为空列表且不加载`() {
        val state = useCase.state.value
        assertTrue(state.cards.isEmpty())
        assertFalse(state.isLoading)
        assertFalse(state.isRefreshing)
        assertNull(state.errorMessage)
        assertTrue(state.hasMore)
    }

    @Test
    fun `刷新事件触发刷新状态`() = runTest {
        useCase.receiveEvent(FeedEvent.OnRefresh)
        assertFalse(useCase.state.value.isRefreshing)
    }

    @Test
    fun `刷新成功全量替换数据`() = runTest {
        useCase.receiveEvent(FeedEvent.OnRefresh)
        val state = useCase.state.value
        assertEquals(10, state.cards.size)
        assertFalse(state.isRefreshing)
        assertEquals(1, state.currentPage)
        assertTrue(state.hasMore)
    }

    @Test
    fun `刷新失败保留现有内容`() = runTest {
        useCase.receiveEvent(FeedEvent.OnRefresh)
        assertEquals(10, useCase.state.value.cards.size)

        val failingUseCase = FeedUseCase(
            FailingFeedRepository(),
            FeedState(cards = useCase.state.value.cards).toStateHolder(),
            MutableServiceRegistryImpl()
        )
        val failingEffects = mutableListOf<BaseEffect>()
        backgroundScope.launch {
            failingUseCase.baseEffect.collect { failingEffects.add(it) }
        }
        runCurrent()

        failingUseCase.receiveEvent(FeedEvent.OnRefresh)
        runCurrent()

        assertEquals(10, failingUseCase.state.value.cards.size)
        assertFalse(failingUseCase.state.value.isRefreshing)
        assertEquals(listOf(BaseEffect.ShowSnackbar("刷新失败，请重试")), failingEffects)
    }

    @Test
    fun `加载更多追加数据`() = runTest {
        useCase.receiveEvent(FeedEvent.OnRefresh)
        useCase.receiveEvent(FeedEvent.OnLoadMore)
        val state = useCase.state.value
        assertEquals(15, state.cards.size)
        assertTrue(state.hasMore)
        assertEquals(2, state.currentPage)
    }

    @Test
    fun `滑动到剩余3个卡片时静默触发加载更多`() = runTest {
        useCase.receiveEvent(FeedEvent.OnRefresh)
        useCase.receiveEvent(FeedEvent.OnPreload(6))
        assertEquals(10, useCase.state.value.cards.size)

        useCase.receiveEvent(FeedEvent.OnPreload(7))
        assertEquals(15, useCase.state.value.cards.size)
    }

    @Test
    fun `加载更多到最后一页`() = runTest {
        useCase.receiveEvent(FeedEvent.OnRefresh)
        useCase.receiveEvent(FeedEvent.OnLoadMore)
        useCase.receiveEvent(FeedEvent.OnLoadMore)
        val state = useCase.state.value
        assertEquals(15, state.cards.size)
        assertFalse(state.hasMore)
    }

    @Test
    fun `刷新进行中时再次触发刷新直接丢弃`() = runTest {
        val delayedRepo = DelayedFeedRepository(1000)
        val delayedUseCase = FeedUseCase(delayedRepo, FeedState().toStateHolder(), MutableServiceRegistryImpl())

        val job = launch {
            delayedUseCase.receiveEvent(FeedEvent.OnRefresh)
        }

        runCurrent()
        assertTrue(delayedUseCase.state.value.isRefreshing)

        delayedUseCase.receiveEvent(FeedEvent.OnRefresh)

        advanceTimeBy(1000)
        runCurrent()
        job.join()

        assertFalse(delayedUseCase.state.value.isRefreshing)
        assertEquals(10, delayedUseCase.state.value.cards.size)
    }

    @Test
    fun `刷新进行中时触发加载更多和预加载直接丢弃`() = runTest {
        val delayedRepo = RecordingDelayedFeedRepository(1000)
        val delayedUseCase = FeedUseCase(delayedRepo, FeedState().toStateHolder(), MutableServiceRegistryImpl())

        val job = launch {
            delayedUseCase.receiveEvent(FeedEvent.OnRefresh)
        }

        runCurrent()
        assertTrue(delayedUseCase.state.value.isRefreshing)

        delayedUseCase.receiveEvent(FeedEvent.OnLoadMore)
        delayedUseCase.receiveEvent(FeedEvent.OnPreload(7))

        advanceTimeBy(1000)
        runCurrent()
        job.join()

        assertEquals(listOf(0), delayedRepo.requestedPages)
        assertFalse(delayedUseCase.state.value.isRefreshing)
        assertEquals(10, delayedUseCase.state.value.cards.size)
    }

    @Test
    fun `加载更多进行中时再次触发加载更多直接丢弃`() = runTest {
        useCase.receiveEvent(FeedEvent.OnRefresh)
        assertEquals(10, useCase.state.value.cards.size)

        val delayedRepo = DelayedFeedRepository(1000)
        val stateHolder = useCase.state.value.copy().toStateHolder()
        val delayedUseCase = FeedUseCase(delayedRepo, stateHolder, MutableServiceRegistryImpl())

        val job = launch {
            delayedUseCase.receiveEvent(FeedEvent.OnLoadMore)
        }

        runCurrent()
        assertTrue(delayedUseCase.state.value.isLoading)

        delayedUseCase.receiveEvent(FeedEvent.OnLoadMore)

        advanceTimeBy(1000)
        runCurrent()
        job.join()

        assertFalse(delayedUseCase.state.value.isLoading)
        assertEquals(15, delayedUseCase.state.value.cards.size)
    }

    @Test
    fun `加载更多进行中时触发刷新和预加载直接丢弃`() = runTest {
        useCase.receiveEvent(FeedEvent.OnRefresh)
        assertEquals(10, useCase.state.value.cards.size)

        val delayedRepo = RecordingDelayedFeedRepository(1000)
        val delayedUseCase = FeedUseCase(
            delayedRepo,
            useCase.state.value.copy().toStateHolder(),
            MutableServiceRegistryImpl()
        )

        val job = launch {
            delayedUseCase.receiveEvent(FeedEvent.OnLoadMore)
        }

        runCurrent()
        assertTrue(delayedUseCase.state.value.isLoading)

        delayedUseCase.receiveEvent(FeedEvent.OnRefresh)
        delayedUseCase.receiveEvent(FeedEvent.OnPreload(7))

        advanceTimeBy(1000)
        runCurrent()
        job.join()

        assertEquals(listOf(1), delayedRepo.requestedPages)
        assertFalse(delayedUseCase.state.value.isLoading)
        assertEquals(15, delayedUseCase.state.value.cards.size)
    }

    @Test
    fun `加载更多失败保留现有内容`() = runTest {
        useCase.receiveEvent(FeedEvent.OnRefresh)
        assertEquals(10, useCase.state.value.cards.size)

        val failingUseCase = FeedUseCase(
            FailingFeedRepository(),
            FeedState(cards = useCase.state.value.cards, currentPage = 1).toStateHolder(),
            MutableServiceRegistryImpl()
        )
        val failingEffects = mutableListOf<BaseEffect>()
        backgroundScope.launch {
            failingUseCase.baseEffect.collect { failingEffects.add(it) }
        }
        runCurrent()

        failingUseCase.receiveEvent(FeedEvent.OnLoadMore)
        runCurrent()

        assertEquals(10, failingUseCase.state.value.cards.size)
        assertFalse(failingUseCase.state.value.isLoading)
        assertEquals(listOf(BaseEffect.ShowSnackbar("加载失败，请重试")), failingEffects)
    }

    @Test
    fun `无更多内容时加载更多和预加载直接丢弃`() = runTest {
        useCase.receiveEvent(FeedEvent.OnRefresh)
        val existingCards = useCase.state.value.cards
        assertEquals(10, existingCards.size)

        val recordingRepo = RecordingFakeFeedRepository()
        val completedUseCase = FeedUseCase(
            recordingRepo,
            FeedState(cards = existingCards, currentPage = 3, hasMore = false).toStateHolder(),
            MutableServiceRegistryImpl()
        )

        completedUseCase.receiveEvent(FeedEvent.OnLoadMore)
        completedUseCase.receiveEvent(FeedEvent.OnPreload(Int.MAX_VALUE))

        assertTrue(recordingRepo.requestedPages.isEmpty())
        assertEquals(existingCards, completedUseCase.state.value.cards)
        assertFalse(completedUseCase.state.value.hasMore)
    }

    private class DelayedFeedRepository(
        private val delayMs: Long,
    ) : FeedRepository {
        private val fake = FakeFeedRepository()

        override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
            delay(delayMs)
            return fake.fetchFeed(page, pageSize)
        }
    }

    private class RecordingDelayedFeedRepository(
        private val delayMs: Long,
    ) : FeedRepository {
        private val fake = FakeFeedRepository()
        val requestedPages = mutableListOf<Int>()

        override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
            requestedPages += page
            delay(delayMs)
            return fake.fetchFeed(page, pageSize)
        }
    }

    private class RecordingFakeFeedRepository : FeedRepository {
        val requestedPages = mutableListOf<Int>()

        override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
            requestedPages += page
            return FakeFeedRepository().fetchFeed(page, pageSize)
        }
    }

    private class FailingFeedRepository : FeedRepository {
        override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
            return Result.failure(Exception("Network error"))
        }
    }
}
