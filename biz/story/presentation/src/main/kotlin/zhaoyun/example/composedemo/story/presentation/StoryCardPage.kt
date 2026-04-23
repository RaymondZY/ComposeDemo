package zhaoyun.example.composedemo.story.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.merge
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard
import zhaoyun.example.composedemo.story.background.presentation.BackgroundViewModel
import zhaoyun.example.composedemo.story.background.presentation.StoryBackground
import zhaoyun.example.composedemo.story.infobar.presentation.InfoBarArea
import zhaoyun.example.composedemo.story.infobar.presentation.InfoBarViewModel
import zhaoyun.example.composedemo.story.input.presentation.InputArea
import zhaoyun.example.composedemo.story.input.presentation.InputViewModel
import zhaoyun.example.composedemo.story.message.presentation.MessageArea
import zhaoyun.example.composedemo.story.message.presentation.MessageViewModel

@Composable
fun StoryCardPage(
    card: StoryCard,
) {
    val storyViewModel: StoryCardViewModel = koinViewModel()

    val messageViewModel: MessageViewModel = koinViewModel {
        parametersOf(storyViewModel.messageReducer)
    }
    val infoBarViewModel: InfoBarViewModel = koinViewModel {
        parametersOf(storyViewModel.infoBarReducer, card.cardId)
    }
    val inputViewModel: InputViewModel = koinViewModel {
        parametersOf(storyViewModel.inputReducer)
    }
    val backgroundViewModel: BackgroundViewModel = koinViewModel {
        parametersOf(storyViewModel.backgroundReducer)
    }

    val state by storyViewModel.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        StoryBackground(state = state.background)
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(1f))
            MessageArea(
                state = state.message,
                onEvent = { messageViewModel.onEvent(it) },
            )
            InfoBarArea(
                state = state.infoBar,
                onEvent = { infoBarViewModel.onEvent(it) },
            )
            InputArea(
                state = state.input,
                onEvent = { inputViewModel.onEvent(it) },
            )
        }
    }
}
