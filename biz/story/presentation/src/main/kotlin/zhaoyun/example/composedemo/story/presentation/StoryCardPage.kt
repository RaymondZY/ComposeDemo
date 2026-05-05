package zhaoyun.example.composedemo.story.presentation

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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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
    // InputArea 底部距窗口顶部的 layout y 坐标（px），键盘收起时（imeBottom≈0）测量并锁定。
    // 键盘弹出期间不更新，避免 graphicsLayer 变换污染 positionInRoot 读数引发反馈循环振荡。
    var inputAreaBottom by remember { mutableFloatStateOf(0f) }
    val imeBottom = WindowInsets.ime.getBottom(density).toFloat()
    val safetyMarginPx = with(density) { 10.dp.toPx() }
    // 坐标系：y 轴向下，原点为窗口左上角。
    //   distanceToBottom = windowHeight - inputAreaBottom  → InputArea 底部到窗口底部的距离
    //   键盘占据窗口底部 imeBottom px，键盘顶部 y = windowHeight - imeBottom
    //   侵入量 = imeBottom - distanceToBottom = 键盘顶部超过 InputArea 底部的距离
    //   加上 safetyMarginPx 确保键盘顶部与 InputArea 底部之间始终保留 10dp 间距
    //   intrusion = imeBottom - (windowHeight - inputAreaBottom) + safetyMarginPx
    // 仅当 inputAreaBottom 在窗口范围内（0 < x < windowHeight）时计算，
    // 排除 VerticalPager 中屏幕外 card（其 positionInRoot.y 超出 windowHeight 导致公式溢出）。
    val intrusion = if (inputAreaBottom > 0f && inputAreaBottom < windowHeight)
        maxOf(0f, imeBottom - (windowHeight - inputAreaBottom) + safetyMarginPx)
    else 0f

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

    Box(modifier = Modifier.fillMaxSize()) {
        StoryBackground(viewModel = backgroundViewModel)

        // 前景内容（渐变遮罩 + UI 元素），背景保持不动
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .graphicsLayer { translationY = -intrusion }
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
                            // graphicsLayer 变换会污染 positionInRoot 的读数。
                            // 只在 imeBottom≈0（键盘收起、intrusion=0、graphicsLayer 无偏移）
                            // 时采样，保证拿到的是纯 layout 坐标。
                            if (imeBottom < 1f) {
                                inputAreaBottom = coords.positionInRoot().y + coords.size.height
                            }
                        },
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 右侧：垂直图标区域（在 InfoBarArea 内部处理）
            }
        }
    }
}
