package zhaoyun.example.composedemo.service.feed.mock

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class FakeFeedRepositoryTest {

    @Test
    fun `fetchFeed returns requested number of randomly composed story cards`() = runTest {
        val repository = FakeFeedRepository(
            random = Random(1),
            delayRangeMillis = 0L..0L,
            failureRate = 0.0,
        )

        val result = repository.fetchFeed(page = 0, pageSize = 10)

        assertTrue(result.isSuccess)
        val cards = result.getOrThrow()
        assertEquals(10, cards.size)
        assertTrue(cards.all { it is StoryCard })
        assertEquals(10, cards.map { it.cardId }.distinct().size)
    }

    @Test
    fun `fetchFeed combines fixed content pool into different results across requests`() = runTest {
        val repository = FakeFeedRepository(
            random = Random(2),
            delayRangeMillis = 0L..0L,
            failureRate = 0.0,
        )

        val first = repository.fetchFeed(page = 0, pageSize = 5).getOrThrow()
        val second = repository.fetchFeed(page = 0, pageSize = 5).getOrThrow()

        assertNotEquals(first, second)
    }

    @Test
    fun `fetchFeed can be configured to fail`() = runTest {
        val repository = FakeFeedRepository(
            random = Random(3),
            delayRangeMillis = 0L..0L,
            failureRate = 1.0,
        )

        val result = repository.fetchFeed(page = 0, pageSize = 10)

        assertTrue(result.isFailure)
    }

    @Test
    fun `fetchFeed applies configured network delay`() = runTest {
        val repository = FakeFeedRepository(
            random = Random(4),
            delayRangeMillis = 500L..500L,
            failureRate = 0.0,
        )

        val deferred = async {
            repository.fetchFeed(page = 0, pageSize = 1)
        }
        advanceTimeBy(499L)
        runCurrent()
        assertTrue(deferred.isActive)

        advanceTimeBy(1L)
        runCurrent()
        assertTrue(deferred.await().isSuccess)
    }
}
