package zhaoyun.example.composedemo.feed.presentation

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import zhaoyun.example.composedemo.feed.domain.FeedEvent
import zhaoyun.example.composedemo.feed.domain.FeedState
import zhaoyun.example.composedemo.scaffold.android.MviItemScope
import zhaoyun.example.composedemo.scaffold.android.MviScreen
import zhaoyun.example.composedemo.scaffold.android.screenViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.service.feed.api.model.FeedCard
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard
import zhaoyun.example.composedemo.story.domain.StoryCardState
import zhaoyun.example.composedemo.story.input.domain.InputKeyboardCoordinator
import zhaoyun.example.composedemo.story.presentation.StoryCardPage
import zhaoyun.example.composedemo.story.presentation.StoryCardViewModel
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun FeedScreen(modifier: Modifier = Modifier) {
    MviScreen<FeedViewModel> { viewModel ->
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.sendEvent(FeedEvent.OnRefresh)
        }

        val pagerState = rememberPagerState(pageCount = { state.cards.size })

        val coordinator = koinInject<InputKeyboardCoordinator>()
        val activeBounds by coordinator.activeInputBounds.collectAsStateWithLifecycle()

        val density = LocalDensity.current
        val pullThresholdPx = remember(density) { with(density) { 80.dp.toPx() } }

        Box(modifier = modifier.fillMaxSize()) {
            FeedScreenContent(
                state = state,
                pagerState = pagerState,
                onSendEvent = viewModel::sendEvent,
                pullThresholdPx = pullThresholdPx,
                cardContent = { card ->
                    if (card is StoryCard) {
                        key(card.cardId) {
                            MviItemScope {
                                val storyViewModel: StoryCardViewModel =
                                    screenViewModel(key = card.cardId) {
                                        parametersOf(StoryCardState.from(card).toStateHolder())
                                    }
                                StoryCardPage(viewModel = storyViewModel, card = card)
                            }
                        }
                    }
                },
            )

            // Keyboard is open: cover only the area outside InputArea.
            activeBounds?.let { bounds ->
                var overlayPositionInRoot by remember { mutableStateOf(Offset.Zero) }
                var overlaySize by remember { mutableStateOf(IntSize.Zero) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { overlayPositionInRoot = it.positionInRoot() }
                        .onSizeChanged { overlaySize = it },
                ) {
                    val left = floor(bounds.left - overlayPositionInRoot.x).roundToInt()
                    val top = floor(bounds.top - overlayPositionInRoot.y).roundToInt()
                    val right = ceil(bounds.right - overlayPositionInRoot.x).roundToInt()
                    val bottom = ceil(bounds.bottom - overlayPositionInRoot.y).roundToInt()
                    val clampedLeft = left.coerceIn(0, overlaySize.width)
                    val clampedTop = top.coerceIn(0, overlaySize.height)
                    val clampedRight = right.coerceIn(0, overlaySize.width)
                    val clampedBottom = bottom.coerceIn(0, overlaySize.height)
                    val middleHeight = max(0, clampedBottom - clampedTop)

                    DismissKeyboardRegion(
                        offset = IntOffset.Zero,
                        size = IntSize(overlaySize.width, clampedTop),
                        onDismiss = coordinator::requestDismiss,
                    )
                    DismissKeyboardRegion(
                        offset = IntOffset(0, clampedBottom),
                        size = IntSize(overlaySize.width, overlaySize.height - clampedBottom),
                        onDismiss = coordinator::requestDismiss,
                    )
                    DismissKeyboardRegion(
                        offset = IntOffset(0, clampedTop),
                        size = IntSize(clampedLeft, middleHeight),
                        onDismiss = coordinator::requestDismiss,
                    )
                    DismissKeyboardRegion(
                        offset = IntOffset(clampedRight, clampedTop),
                        size = IntSize(overlaySize.width - clampedRight, middleHeight),
                        onDismiss = coordinator::requestDismiss,
                    )
                }
            }
        }
    }
}

@Composable
internal fun FeedScreenContent(
    state: FeedState,
    pagerState: PagerState,
    onSendEvent: (FeedEvent) -> Unit,
    pullThresholdPx: Float,
    cardContent: @Composable (FeedCard) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (state.cards.isNotEmpty()) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("feed_vertical_pager"),
                beyondViewportPageCount = 1,
            ) { page ->
                // 滑动到剩余 3 个卡片时静默触发加载更多
                if (page >= state.cards.size - 3) {
                    LaunchedEffect(page) {
                        onSendEvent(FeedEvent.OnPreload(page))
                    }
                }

                cardContent(state.cards[page])
            }
        } else if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("feed_empty_loading"),
            )
        }

        // 顶部下拉刷新触发区域
        if (state.cards.isNotEmpty() && !state.isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.TopCenter)
                    .testTag("feed_pull_refresh_area")
                    .pointerInput(pullThresholdPx, state.isRefreshing) {
                        if (state.isRefreshing) return@pointerInput
                        var accumulatedDrag = 0f
                        detectVerticalDragGestures(
                            onDragStart = { accumulatedDrag = 0f },
                            onDragEnd = {
                                if (accumulatedDrag > pullThresholdPx) {
                                    onSendEvent(FeedEvent.OnRefresh)
                                }
                                accumulatedDrag = 0f
                            },
                            onVerticalDrag = { _, dragAmount ->
                                accumulatedDrag += dragAmount
                            }
                        )
                    }
            )
        }

        // 刷新指示器（Header）
        if (state.isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .testTag("feed_refresh_indicator"),
            )
        }

        // Footer：加载中 / 没有更多内容
        if (state.cards.isNotEmpty()) {
            when {
                state.isLoading && !state.isRefreshing -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .testTag("feed_loading_indicator"),
                    )
                }

                !state.hasMore -> {
                    Text(
                        text = "没有更多内容",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .testTag("feed_no_more_content"),
                    )
                }
            }
        }

    }
}

@Composable
private fun DismissKeyboardRegion(
    offset: IntOffset,
    size: IntSize,
    onDismiss: () -> Unit,
) {
    if (size.width <= 0 || size.height <= 0) return

    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset { offset }
            .size(
                width = with(density) { size.width.toDp() },
                height = with(density) { size.height.toDp() },
            )
            .pointerInput(onDismiss) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val downChange = event.changes.firstOrNull { it.changedToDown() }
                        if (downChange != null) {
                            downChange.consume()
                            onDismiss()
                            do {
                                val nextEvent = awaitPointerEvent(PointerEventPass.Initial)
                                nextEvent.changes.forEach { it.consume() }
                            } while (nextEvent.changes.any { it.pressed })
                        }
                    }
                }
            },
    )
}
