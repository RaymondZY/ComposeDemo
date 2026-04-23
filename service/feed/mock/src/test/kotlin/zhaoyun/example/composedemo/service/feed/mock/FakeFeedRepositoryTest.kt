package zhaoyun.example.composedemo.service.feed.mock

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard

class FakeFeedRepositoryTest {

    private val repository = FakeFeedRepository()

    @Test
    fun `fetchFeed返回预定义列表`() = runTest {
        val result = repository.fetchFeed(page = 0, pageSize = 10)
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().size)
        assertTrue(result.getOrThrow()[0] is StoryCard)
    }

    @Test
    fun `fetchFeed第二页返回空列表`() = runTest {
        val result = repository.fetchFeed(page = 1, pageSize = 10)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }
}
