package zhaoyun.example.composedemo.story.storypanel.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import org.koin.core.parameter.parametersOf
import zhaoyun.example.composedemo.scaffold.platform.MviScreen
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.story.storypanel.core.StoryPanelEffect
import zhaoyun.example.composedemo.story.storypanel.core.StoryPanelState

@Composable
fun StoryPanelScreen(
    cardId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MviScreen<StoryPanelViewModel>(
        parameters = {
            parametersOf(StoryPanelState(cardId = cardId).toStateHolder())
        },
    ) { viewModel ->
        LaunchedEffect(viewModel) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is StoryPanelEffect.NavigateBack -> onNavigateBack()
                }
            }
        }

        Box(
            modifier = modifier.fillMaxSize(),
        ) {
            // 空页面占位，保留后续扩展逻辑
        }
    }
}
