package zhaoyun.example.composedemo.story.presentation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
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
    val density = LocalDensity.current
    val windowHeight = LocalView.current.height.toFloat()
    var inputAreaBottom by remember { mutableFloatStateOf(0f) }
    val imeBottom = WindowInsets.ime.getBottom(density).toFloat()
    val safetyMarginPx = with(density) { 10.dp.toPx() }
    val intrusion = if (inputAreaBottom > 0f)
        maxOf(0f, imeBottom - (windowHeight - inputAreaBottom) - safetyMarginPx)
    else 0f

    Log.d("DEBUG", "StoryCardPage recomposition")
    Log.d("DEBUG", "density = $density")
    Log.d("DEBUG", "windowHeight = $windowHeight")
    Log.d("DEBUG", "inputAreaBottom = $inputAreaBottom")
    Log.d("DEBUG", "imeBottom = $imeBottom")
    Log.d("DEBUG", "safetyMarginPx = $safetyMarginPx")
    Log.d("DEBUG", "intrusion = $intrusion")

    val messageViewModel: MessageViewModel = screenViewModel(card.cardId) {
        parametersOf(viewModel.messageStateHolder)
    }

    val infoBarViewModel: InfoBarViewModel = screenViewModel(card.cardId) {
        parametersOf(card.cardId, viewModel.infoBarStateHolder)
    }

    val inputViewModel: InputViewModel = screenViewModel(card.cardId) {
        parametersOf(viewModel.inputStateHolder)
    }

    val backgroundViewModel: BackgroundViewModel = screenViewModel(card.cardId) {
        parametersOf(viewModel.backgroundStateHolder)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
//            .graphicsLayer { translationY = -intrusion },
    ) {
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
                    InputArea(
                        viewModel = inputViewModel,
                        modifier = Modifier.onGloballyPositioned { coords ->
                            Log.d("DEBUG", "card: ${card.cardId} onGloballyPositioned")
                            Log.d("DEBUG", "isAttached: ${coords.isAttached}")
                            Log.d("DEBUG", "positionInParent: ${coords.positionInParent()}")
                            Log.d("DEBUG", "positionOnScreen: ${coords.positionOnScreen()}")
                            Log.d("DEBUG", "positionInRoot: ${coords.positionInRoot()}")
                            Log.d("DEBUG", "boundsInWindow: ${coords.boundsInWindow()}")
                        },
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 右侧：垂直图标区域（在 InfoBarArea 内部处理）
            }
        }
    }
}
