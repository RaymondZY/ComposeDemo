package zhaoyun.example.composedemo.feed.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.androidx.compose.koinViewModel
import zhaoyun.example.composedemo.feed.domain.FeedEvent
import zhaoyun.example.composedemo.scaffold.android.MviScreen

@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = koinViewModel()
) {
    MviScreen(
        viewModel = viewModel,
        initEvent = FeedEvent.OnRefresh
    ) { state, onEvent ->
        FeedPage(
            state = state,
            onEvent = onEvent,
            modifier = modifier
        )
    }
}
