package zhaoyun.example.composedemo.feed.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import zhaoyun.example.composedemo.feed.domain.FeedEvent
import zhaoyun.example.composedemo.scaffold.android.MviItemScope
import zhaoyun.example.composedemo.scaffold.android.MviScreen
import zhaoyun.example.composedemo.scaffold.android.screenViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard
import zhaoyun.example.composedemo.story.domain.StoryCardState
import zhaoyun.example.composedemo.story.input.domain.InputKeyboardCoordinator
import zhaoyun.example.composedemo.story.presentation.StoryCardPage
import zhaoyun.example.composedemo.story.presentation.StoryCardViewModel

@Composable
fun FeedScreen(modifier: Modifier = Modifier) {
    MviScreen<FeedViewModel> { viewModel ->
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(Unit) {
            viewModel.receiveEvent(FeedEvent.OnRefresh)
        }

        val pagerState = rememberPagerState(pageCount = { state.cards.size })

        val coordinator = koinInject<InputKeyboardCoordinator>()
        val activeBounds by coordinator.activeInputBounds.collectAsStateWithLifecycle()

        Box(modifier = modifier.fillMaxSize()) {
            if (state.cards.isNotEmpty()) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    val card = state.cards[page]
                    if (card is StoryCard) {
                        key(card.cardId) {
                            MviItemScope {
                                val storyViewModel: StoryCardViewModel = screenViewModel(key = card.cardId) {
                                    parametersOf(StoryCardState.from(card).toStateHolder())
                                }
                                StoryCardPage(viewModel = storyViewModel, card = card)
                            }
                        }
                    }
                }
            } else if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // 键盘展开（有 InputArea 持焦）期间叠加全屏拦截层：
            // - down 落在 InputArea bounds 外：消费整段手势 + dismiss（即 VerticalPager 不会响应翻页）
            // - down 落在 bounds 内：不消费，事件透传给下层 InputArea（光标拖动等正常工作）
            activeBounds?.let { bounds ->
                var overlayPositionInRoot by remember { mutableStateOf(Offset.Zero) }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { overlayPositionInRoot = it.positionInRoot() }
                        .pointerInput(bounds) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val downChange = event.changes.firstOrNull { it.changedToDown() }
                                    if (downChange != null) {
                                        val rootPos = downChange.position + overlayPositionInRoot
                                        if (!bounds.contains(rootPos.x, rootPos.y)) {
                                            downChange.consume()
                                            coordinator.requestDismiss()
                                            // 把整段手势的后续 move/up 也消费，避免传给 VerticalPager 引发翻页
                                            do {
                                                val nextEvent =
                                                    awaitPointerEvent(PointerEventPass.Initial)
                                                nextEvent.changes.forEach { it.consume() }
                                            } while (nextEvent.changes.any { it.pressed })
                                        }
                                    }
                                }
                            }
                        }
                )
            }

            if (state.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            if (state.isLoading && !state.isRefreshing && state.cards.isNotEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}
