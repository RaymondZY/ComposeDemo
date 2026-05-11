package zhaoyun.example.composedemo.feed.platform.di

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import zhaoyun.example.composedemo.feed.platform.FeedViewModel
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.service.feed.api.FeedRepository
import zhaoyun.example.composedemo.service.feed.api.model.FeedCard

class FeedPlatformModuleTest {

    @Test
    fun `feed modules only expose the platform module`() {
        assertEquals(listOf(feedPlatformModule), feedModules)
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

        assertNull(viewModel.state.value.errorMessage)
    }
}
