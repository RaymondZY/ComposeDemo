package zhaoyun.example.composedemo.story.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.core.parameter.parametersOf
import zhaoyun.example.composedemo.scaffold.android.ServiceRegistryProvider
import zhaoyun.example.composedemo.scaffold.android.screenViewModel
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
    // Screen 级别建立 ServiceRegistry 作用域
    ServiceRegistryProvider {

        val storyViewModel: StoryCardViewModel = screenViewModel()

        val messageViewModel: MessageViewModel = screenViewModel {
            parametersOf(storyViewModel.messageStateHolder)
        }
        val infoBarViewModel: InfoBarViewModel = screenViewModel {
            parametersOf(storyViewModel.infoBarStateHolder, card.cardId)
        }
        val inputViewModel: InputViewModel = screenViewModel {
            parametersOf(storyViewModel.inputStateHolder)
        }
        val backgroundViewModel: BackgroundViewModel = screenViewModel {
            parametersOf(storyViewModel.backgroundStateHolder)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            StoryBackground(viewModel = backgroundViewModel)
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.weight(1f))
                MessageArea(viewModel = messageViewModel)
                InfoBarArea(viewModel = infoBarViewModel)
                InputArea(viewModel = inputViewModel)
            }
        }
    }
}
