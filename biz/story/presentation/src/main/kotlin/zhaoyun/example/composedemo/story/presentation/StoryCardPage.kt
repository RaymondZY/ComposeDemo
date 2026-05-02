package zhaoyun.example.composedemo.story.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.koin.core.parameter.parametersOf
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
    viewModel: StoryCardViewModel,
    card: StoryCard,
) {
    val messageViewModel: MessageViewModel = screenViewModel(card) {
        parametersOf(viewModel.messageStateHolder)
    }

    val infoBarViewModel: InfoBarViewModel = screenViewModel(card) {
        parametersOf(card.cardId, viewModel.infoBarStateHolder)
    }

    val inputViewModel: InputViewModel = screenViewModel(card) {
        parametersOf(viewModel.inputStateHolder)
    }

    val backgroundViewModel: BackgroundViewModel = screenViewModel(card) {
        parametersOf(viewModel.backgroundStateHolder)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StoryBackground(viewModel = backgroundViewModel)

        // 底部渐变遮罩
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f),
                        ),
                    ),
                ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                // 左侧：Message + InfoBar + Input
                Column(
                    modifier = Modifier.weight(1f),
                ) {
                    MessageArea(viewModel = messageViewModel)
                    InfoBarArea(viewModel = infoBarViewModel)
                    InputArea(viewModel = inputViewModel)
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 右侧：垂直图标区域（在 InfoBarArea 内部处理）
            }
        }
    }
}
