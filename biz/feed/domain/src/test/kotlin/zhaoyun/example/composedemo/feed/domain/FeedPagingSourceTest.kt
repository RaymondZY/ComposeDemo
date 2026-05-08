package zhaoyun.example.composedemo.feed.domain

import androidx.paging.PagingSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.service.feed.api.FeedRepository
import zhaoyun.example.composedemo.service.feed.api.model.FeedCard
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard

class FeedPagingSourceTest {

    @Test
    fun `loads first page with next key`() = runTest {
        val repository = RecordingFeedRepository(
            pages = mapOf(0 to listOf(createStoryCard(0), createStoryCard(1))),
        )
        val source = FeedPagingSource(repository)

        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false,
            ),
        )

        assertEquals(listOf(0 to 10), repository.requests)
        assertEquals(
            PagingSource.LoadResult.Page(
                data = listOf(createStoryCard(0), createStoryCard(1)),
                prevKey = null,
                nextKey = 1,
            ),
            result,
        )
    }

    @Test
    fun `loads appended page with previous and next keys`() = runTest {
        val repository = RecordingFeedRepository(
            pages = mapOf(2 to listOf(createStoryCard(20))),
        )
        val source = FeedPagingSource(repository)

        val result = source.load(
            PagingSource.LoadParams.Append(
                key = 2,
                loadSize = 10,
                placeholdersEnabled = false,
            ),
        )

        assertEquals(listOf(2 to 10), repository.requests)
        assertEquals(
            PagingSource.LoadResult.Page(
                data = listOf(createStoryCard(20)),
                prevKey = 1,
                nextKey = 3,
            ),
            result,
        )
    }

    @Test
    fun `empty page ends pagination`() = runTest {
        val repository = RecordingFeedRepository(pages = mapOf(1 to emptyList()))
        val source = FeedPagingSource(repository)

        val result = source.load(
            PagingSource.LoadParams.Append(
                key = 1,
                loadSize = 10,
                placeholdersEnabled = false,
            ),
        )

        assertEquals(
            PagingSource.LoadResult.Page<Int, FeedCard>(
                data = emptyList(),
                prevKey = 0,
                nextKey = null,
            ),
            result,
        )
    }

    @Test
    fun `repository failure returns paging error`() = runTest {
        val source = FeedPagingSource(FailingFeedRepository())

        val result = source.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false,
            ),
        )

        assertTrue(result is PagingSource.LoadResult.Error)
    }

    @Test
    fun `refresh key uses closest page`() {
        val source = FeedPagingSource(RecordingFeedRepository(emptyMap()))
        val state = PagingSource.LoadResult.Page<Int, FeedCard>(
            data = listOf(createStoryCard(0)),
            prevKey = 1,
            nextKey = 3,
        )

        val refreshKey = source.getRefreshKey(
            androidx.paging.PagingState(
                pages = listOf(state),
                anchorPosition = 0,
                config = androidx.paging.PagingConfig(pageSize = 10),
                leadingPlaceholderCount = 0,
            ),
        )

        assertEquals(2, refreshKey)
    }

    private class RecordingFeedRepository(
        private val pages: Map<Int, List<FeedCard>>,
    ) : FeedRepository {
        val requests = mutableListOf<Pair<Int, Int>>()

        override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
            requests += page to pageSize
            return Result.success(pages[page].orEmpty())
        }
    }

    private class FailingFeedRepository : FeedRepository {
        override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
            return Result.failure(IllegalStateException("failed"))
        }
    }

    private fun createStoryCard(index: Int): StoryCard = StoryCard(
        cardId = "$index",
        backgroundImageUrl = "",
        characterName = "Character $index",
        characterSubtitle = null,
        dialogueText = "Dialogue $index",
        storyTitle = "Story $index",
        creatorName = "Creator",
        creatorHandle = "@creator",
        likes = index,
        shares = index,
        comments = index,
        isLiked = false,
    )
}
