package zhaoyun.example.composedemo.feed.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import zhaoyun.example.composedemo.feed.domain.FeedEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.service.feed.api.FeedRepository
import zhaoyun.example.composedemo.service.feed.api.model.FeedCard
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard

class FeedScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun feedScreen_initialPagingLoad_displaysFirstCard() {
        val viewModel = FeedViewModel(
            feedRepository = createFakeRepository(totalItems = 10),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        createContent(viewModel)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching { composeRule.onNodeWithTag("card_0").assertIsDisplayed() }.isSuccess
        }
        composeRule.onNodeWithTag("card_0").assertIsDisplayed()
    }

    @Test
    fun feedScreen_pullDownGesture_triggersRefresh() {
        val viewModel = FeedViewModel(
            feedRepository = SecondRefreshDelayedFeedRepository(
                delegate = createFakeRepository(totalItems = 10),
                secondRefreshDelayMillis = 3_000,
            ),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        createContent(viewModel)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching { composeRule.onNodeWithTag("card_0").assertIsDisplayed() }.isSuccess
        }

        composeRule.onNodeWithTag("feed_pull_refresh_area").performTouchInput {
            swipeDown(startY = centerY, endY = bottom - 1f, durationMillis = 800)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("feed_refresh_indicator").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithTag("feed_refresh_indicator").assertIsDisplayed()
    }

    @Test
    fun feedScreen_appendLoading_showsFooter() {
        val viewModel = FeedViewModel(
            feedRepository = DelayedFeedRepository(
                delegate = createFakeRepository(pageSize = 10, totalItems = 15),
                firstPageDelayMillis = 0,
                nextPageDelayMillis = 3_000,
            ),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        createContent(viewModel)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching { composeRule.onNodeWithTag("card_0").assertIsDisplayed() }.isSuccess
        }

        repeat(8) {
            composeRule.onNodeWithTag("feed_vertical_pager").performTouchInput {
                swipeUp()
            }
            composeRule.waitForIdle()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("feed_loading_indicator").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithTag("feed_loading_indicator").assertIsDisplayed()
    }

    @Test
    fun feedScreen_endOfPagination_showsNoMoreContent() {
        val viewModel = FeedViewModel(
            feedRepository = createFakeRepository(pageSize = 10, totalItems = 10),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        createContent(viewModel)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching { composeRule.onNodeWithTag("card_0").assertIsDisplayed() }.isSuccess
        }

        repeat(8) {
            composeRule.onNodeWithTag("feed_vertical_pager").performTouchInput {
                swipeUp()
            }
            composeRule.waitForIdle()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithTag("feed_no_more_content").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithText("没有更多内容").assertIsDisplayed()
    }

    @Test
    fun feedScreen_refreshError_showsSnackbar() {
        val viewModel = FeedViewModel(
            feedRepository = createFailingRepository(),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        createContent(viewModel)

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("刷新失败，请重试").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithText("刷新失败，请重试").assertIsDisplayed()
    }

    @Test
    fun feedScreen_appendError_showsSnackbar() {
        val viewModel = FeedViewModel(
            feedRepository = object : FeedRepository {
                private val firstPageRepository = createFakeRepository(pageSize = 10, totalItems = 10)

                override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
                    return if (page == 0) {
                        firstPageRepository.fetchFeed(page, pageSize)
                    } else {
                        Result.failure(RuntimeException("Load more error"))
                    }
                }
            },
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        createContent(viewModel)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching { composeRule.onNodeWithTag("card_0").assertIsDisplayed() }.isSuccess
        }

        repeat(8) {
            composeRule.onNodeWithTag("feed_vertical_pager").performTouchInput {
                swipeUp()
            }
            composeRule.waitForIdle()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            runCatching {
                composeRule.onNodeWithText("加载失败，请重试").assertIsDisplayed()
            }.isSuccess
        }
        composeRule.onNodeWithText("加载失败，请重试").assertIsDisplayed()
    }

    @Test
    fun feedScreen_emptyRefreshLoading_showsCenteredLoadingOnly() {
        val viewModel = FeedViewModel(
            feedRepository = DelayedFeedRepository(
                delegate = createFakeRepository(totalItems = 10),
                firstPageDelayMillis = 3_000,
                nextPageDelayMillis = 0,
            ),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        createContent(viewModel)

        composeRule.onNodeWithTag("feed_empty_loading").assertIsDisplayed()
        assert(
            runCatching {
                composeRule.onNodeWithTag("feed_vertical_pager").assertIsDisplayed()
            }.isFailure,
        )
    }

    private fun createContent(viewModel: FeedViewModel) {
        composeRule.setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val lazyPagingItems = viewModel.pagingData.collectAsLazyPagingItems()
            val pagerState = rememberPagerState(pageCount = { lazyPagingItems.itemCount })

            LaunchedEffect(viewModel) {
                viewModel.baseEffect.collect { effect ->
                    if (effect is BaseEffect.ShowSnackbar) {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
            }

            LaunchedEffect(lazyPagingItems.loadState.refresh) {
                if (lazyPagingItems.loadState.refresh is LoadState.Error) {
                    viewModel.sendEvent(FeedEvent.OnRefreshFailed)
                }
            }

            LaunchedEffect(lazyPagingItems.loadState.append) {
                if (lazyPagingItems.loadState.append is LoadState.Error) {
                    viewModel.sendEvent(FeedEvent.OnLoadMoreFailed)
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                FeedScreenContent(
                    lazyPagingItems = lazyPagingItems,
                    pagerState = pagerState,
                    cardContent = { card ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("card_${card.cardId}"),
                        )
                    },
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    private fun createFakeRepository(
        pageSize: Int = 10,
        totalItems: Int = 15,
    ): FeedRepository = object : FeedRepository {
        override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
            val start = page * pageSize
            val end = minOf(start + pageSize, totalItems)
            return if (start < totalItems) {
                Result.success((start until end).map { createStoryCard(it) })
            } else {
                Result.success(emptyList())
            }
        }
    }

    private class DelayedFeedRepository(
        private val delegate: FeedRepository,
        private val firstPageDelayMillis: Long,
        private val nextPageDelayMillis: Long,
    ) : FeedRepository {
        override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
            val delayMillis = if (page == 0) firstPageDelayMillis else nextPageDelayMillis
            delay(delayMillis)
            return delegate.fetchFeed(page, pageSize)
        }
    }

    private class SecondRefreshDelayedFeedRepository(
        private val delegate: FeedRepository,
        private val secondRefreshDelayMillis: Long,
    ) : FeedRepository {
        private var firstPageRequests = 0

        override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
            if (page == 0) {
                firstPageRequests += 1
                if (firstPageRequests > 1) {
                    delay(secondRefreshDelayMillis)
                }
            }
            return delegate.fetchFeed(page, pageSize)
        }
    }

    private fun createFailingRepository(): FeedRepository = object : FeedRepository {
        override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
            return Result.failure(RuntimeException("Network error"))
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
