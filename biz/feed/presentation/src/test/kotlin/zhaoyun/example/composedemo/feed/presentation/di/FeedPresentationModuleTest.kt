package zhaoyun.example.composedemo.feed.presentation.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.feed.presentation.FeedViewModel
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.service.feed.api.FeedRepository
import zhaoyun.example.composedemo.service.feed.api.model.FeedCard

class FeedPresentationModuleTest {

    @Test
    fun `feed modules only expose the presentation module`() {
        assertEquals(listOf(feedPresentationModule), feedModules)
    }

    @Test
    fun `feed view model accepts repository injection`() {
        val viewModel = FeedViewModel(
            feedRepository = object : FeedRepository {
                override suspend fun fetchFeed(
                    page: Int,
                    pageSize: Int,
                ): Result<List<FeedCard>> = Result.success(emptyList())
            },
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        assertTrue(viewModel.state.value.cards.isEmpty())
    }
}
