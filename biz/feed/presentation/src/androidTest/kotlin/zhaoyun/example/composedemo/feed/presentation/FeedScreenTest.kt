package zhaoyun.example.composedemo.feed.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.junit.Rule
import org.junit.Test
import zhaoyun.example.composedemo.feed.domain.FeedEffect
import zhaoyun.example.composedemo.feed.domain.FeedEvent
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.service.feed.api.FeedRepository
import zhaoyun.example.composedemo.service.feed.api.model.FeedCard
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard

class FeedScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

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
        private val delayMillis: Long = 3000,
    ) : FeedRepository {
        override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
            delay(delayMillis)
            return delegate.fetchFeed(page, pageSize)
        }
    }

    private fun createFailingRepository(): FeedRepository = object : FeedRepository {
        override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
            return Result.failure(RuntimeException("Network error"))
        }
    }

    private fun createContent(
        viewModel: FeedViewModel,
        withEffectCollection: Boolean = true,
    ) {
        composeRule.setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val snackbarHostState = remember { SnackbarHostState() }

            if (withEffectCollection) {
                LaunchedEffect(viewModel) {
                    viewModel.effect.collect { effect ->
                        when (effect) {
                            is FeedEffect.ShowRefreshError -> snackbarHostState.showSnackbar("刷新失败，请重试")
                            is FeedEffect.ShowLoadMoreError -> snackbarHostState.showSnackbar("加载失败，请重试")
                        }
                    }
                }
            }

            val pagerState = rememberPagerState(pageCount = { state.cards.size })

            FeedScreenContent(
                state = state,
                pagerState = pagerState,
                onSendEvent = viewModel::sendEvent,
                snackbarHostState = snackbarHostState,
                pullThresholdPx = 80f,
                cardContent = { card ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("card_${card.cardId}")
                    )
                },
            )
        }
    }

    @Test
    fun feedScreen_autoRefresh_loadsCards() {
        val viewModel = FeedViewModel(
            feedRepository = createFakeRepository(totalItems = 10),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        createContent(viewModel)

        // 触发刷新
        viewModel.sendEvent(FeedEvent.OnRefresh)
        composeRule.waitForIdle()

        // 等待刷新完成
        composeRule.waitUntil(timeoutMillis = 5000) {
            viewModel.state.value.cards.isNotEmpty()
        }

        // 验证卡片显示（只检查可见的卡片，VerticalPager 懒加载）
        composeRule.onNodeWithTag("card_0").assertIsDisplayed()
        assert(viewModel.state.value.cards.size == 10)
    }

    @Test
    fun feedScreen_pullDownGesture_triggersRefresh() {
        val viewModel = FeedViewModel(
            feedRepository = DelayedFeedRepository(
                createFakeRepository(totalItems = 10),
                delayMillis = 3000,
            ),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        // 先加载数据
        viewModel.sendEvent(FeedEvent.OnRefresh)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5000) {
            viewModel.state.value.cards.isNotEmpty()
        }

        createContent(viewModel)
        composeRule.waitForIdle()

        // 验证下拉区域存在
        composeRule.onNodeWithTag("feed_pull_refresh_area").assertIsDisplayed()

        // 在顶部区域执行下拉手势（滑动距离 > 阈值）
        composeRule.onNodeWithTag("feed_pull_refresh_area").performTouchInput {
            down(center)
            moveBy(Offset(0f, 500f))
            up()
        }

        // 验证刷新指示器出现（延迟 Repository 让刷新状态持续更久）
        composeRule.waitUntil(timeoutMillis = 5000) {
            runCatching {
                composeRule.onNodeWithTag("feed_refresh_indicator").assertExists()
            }.isSuccess
        }
        composeRule.onNodeWithTag("feed_refresh_indicator").assertIsDisplayed()
    }

    @Test
    fun feedScreen_preload_loadsMoreCards() {
        val viewModel = FeedViewModel(
            feedRepository = createFakeRepository(pageSize = 10, totalItems = 15),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        // 先加载第一页
        viewModel.sendEvent(FeedEvent.OnRefresh)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5000) {
            viewModel.state.value.cards.size == 10
        }

        createContent(viewModel)
        composeRule.waitForIdle()

        // 发送预加载事件（第 7 张卡片，剩余 3 张触发阈值）
        viewModel.sendEvent(FeedEvent.OnPreload(7))
        composeRule.waitForIdle()

        // 等待加载完成
        composeRule.waitUntil(timeoutMillis = 5000) {
            viewModel.state.value.cards.size == 15
        }

        // 验证卡片增加（只检查可见卡片，不检查懒加载的页面）
        assert(viewModel.state.value.cards.size == 15)
        composeRule.onNodeWithTag("card_0").assertIsDisplayed()
    }

    @Test
    fun feedScreen_footer_loadingIndicator() {
        val viewModel = FeedViewModel(
            feedRepository = DelayedFeedRepository(
                createFakeRepository(),
                delayMillis = 3000,
            ),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        // 先加载数据，确保卡片存在才能显示 Footer
        viewModel.sendEvent(FeedEvent.OnRefresh)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5000) {
            viewModel.state.value.cards.isNotEmpty()
        }

        createContent(viewModel)
        composeRule.waitForIdle()

        // 初始状态不应显示加载指示器
        composeRule.onNodeWithTag("feed_loading_indicator").assertDoesNotExist()

        // 手动发送加载更多事件（延迟 Repository 让加载状态持续更久）
        viewModel.sendEvent(FeedEvent.OnLoadMore)
        composeRule.waitForIdle()

        // 等待加载指示器出现
        composeRule.waitUntil(timeoutMillis = 5000) {
            runCatching {
                composeRule.onNodeWithTag("feed_loading_indicator").assertExists()
            }.isSuccess
        }
        composeRule.onNodeWithTag("feed_loading_indicator").assertIsDisplayed()
    }

    @Test
    fun feedScreen_footer_noMoreContent() {
        val viewModel = FeedViewModel(
            feedRepository = createFakeRepository(pageSize = 10, totalItems = 10),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        createContent(viewModel)

        // 加载第一页
        viewModel.sendEvent(FeedEvent.OnRefresh)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5000) {
            viewModel.state.value.cards.isNotEmpty()
        }

        // 加载第二页（空，触发 hasMore = false）
        viewModel.sendEvent(FeedEvent.OnLoadMore)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5000) {
            !viewModel.state.value.hasMore
        }

        // 验证"没有更多内容"显示
        composeRule.onNodeWithTag("feed_no_more_content").assertIsDisplayed()
        composeRule.onNodeWithText("没有更多内容").assertIsDisplayed()
    }

    @Test
    fun feedScreen_error_showsSnackbar() {
        val viewModel = FeedViewModel(
            feedRepository = createFailingRepository(),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        createContent(viewModel)

        // 触发刷新失败
        viewModel.sendEvent(FeedEvent.OnRefresh)
        composeRule.waitForIdle()

        // 等待 Snackbar 显示
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("feed_snackbar_host").assertExists()
            }.isSuccess
        }

        // 验证 Snackbar 文本
        composeRule.onNodeWithText("刷新失败，请重试").assertIsDisplayed()
    }

    @Test
    fun feedScreen_emptyState_showsLoading() {
        // 直接构造空列表 + isLoading 状态，不经过 ViewModel/UseCase
        composeRule.setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            val pagerState = rememberPagerState(pageCount = { 0 })
            FeedScreenContent(
                state = zhaoyun.example.composedemo.feed.domain.FeedState(isLoading = true),
                pagerState = pagerState,
                onSendEvent = {},
                snackbarHostState = snackbarHostState,
                pullThresholdPx = 80f,
                cardContent = {},
            )
        }

        // 验证空状态加载指示器显示
        composeRule.onNodeWithTag("feed_empty_loading").assertIsDisplayed()
    }

    @Test
    fun feedScreen_loadMoreError_showsSnackbar() {
        val firstPageRepo = createFakeRepository(pageSize = 10, totalItems = 10)
        val viewModel = FeedViewModel(
            feedRepository = object : FeedRepository {
                override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
                    return if (page == 0) {
                        firstPageRepo.fetchFeed(page, pageSize)
                    } else {
                        Result.failure(RuntimeException("Load more error"))
                    }
                }
            },
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        createContent(viewModel)

        // 加载第一页成功
        viewModel.sendEvent(FeedEvent.OnRefresh)
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5000) {
            viewModel.state.value.cards.size == 10
        }

        // 加载第二页失败
        viewModel.sendEvent(FeedEvent.OnLoadMore)
        composeRule.waitForIdle()

        // 等待 Snackbar 显示
        composeRule.waitUntil(timeoutMillis = 3000) {
            runCatching {
                composeRule.onNodeWithTag("feed_snackbar_host").assertExists()
            }.isSuccess
        }

        // 验证 Snackbar 文本
        composeRule.onNodeWithText("加载失败，请重试").assertIsDisplayed()
    }
}
