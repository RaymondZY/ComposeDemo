package zhaoyun.example.composedemo.feed.presentation

import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
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
import zhaoyun.example.composedemo.story.input.domain.InputKeyboardCoordinator
import zhaoyun.example.composedemo.story.domain.StoryCardState
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
        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { coordinator.requestDismiss() }
                }
        ) {
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
