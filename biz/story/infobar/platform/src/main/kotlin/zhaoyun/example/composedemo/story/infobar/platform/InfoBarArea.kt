package zhaoyun.example.composedemo.story.infobar.platform

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import zhaoyun.example.composedemo.story.infobar.core.InfoBarEffect
import zhaoyun.example.composedemo.story.infobar.core.InfoBarEvent
import zhaoyun.example.composedemo.story.storypanel.platform.StoryPanelScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBarArea(
    viewModel: InfoBarViewModel,
    cardId: String,
    onSharePanelRequested: (String) -> Unit,
    onCommentPanelRequested: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showHistorySheet by remember { mutableStateOf(false) }
    var showDetailPanel by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is InfoBarEffect.OpenSharePanel -> onSharePanelRequested(effect.cardId)
                is InfoBarEffect.OpenCommentPanel -> onCommentPanelRequested(effect.cardId)
                is InfoBarEffect.ShowHistory -> showHistorySheet = true
                is InfoBarEffect.NavigateToStoryDetail -> showDetailPanel = true
            }
        }
    }

    Column(
        modifier = modifier.padding(top = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧：作者信息（可点击进入故事详情占位）
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.sendEvent(InfoBarEvent.OnStoryTitleClicked) },
            ) {
                Text(
                    text = state.creatorName.ifEmpty { state.storyTitle.ifEmpty { "未命名故事" } },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                )
                if (state.creatorHandle.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = state.creatorHandle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 右侧：横向排列的图标按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IconButtonHorizontal(
                    icon = {
                        Icon(
                            imageVector = if (state.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (state.isLiked) Color(0xFFFF6B6B) else Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    count = formatCount(state.likes),
                    onClick = { viewModel.sendEvent(InfoBarEvent.OnLikeClicked) },
                )

                IconButtonHorizontal(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    count = formatCount(state.shares),
                    onClick = { viewModel.sendEvent(InfoBarEvent.OnShareClicked) },
                )

                IconButtonHorizontal(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.ThumbUp,
                            contentDescription = "Comment",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    count = formatCount(state.comments),
                    onClick = { viewModel.sendEvent(InfoBarEvent.OnCommentClicked) },
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { viewModel.sendEvent(InfoBarEvent.OnHistoryClicked) },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "History",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "历史",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }

    if (showHistorySheet) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) { }
        }
    }

    if (showDetailPanel) {
        androidx.compose.material3.ModalBottomSheet(
            onDismissRequest = { showDetailPanel = false },
            sheetState = androidx.compose.material3.rememberModalBottomSheetState(),
        ) {
            StoryPanelScreen(
                cardId = cardId,
                onNavigateBack = { showDetailPanel = false },
            )
        }
    }
}

@Composable
private fun IconButtonHorizontal(
    icon: @Composable () -> Unit,
    count: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        icon()
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = count,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = Color.White,
        )
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 10000 -> "${count / 10000}万"
        count >= 1000 -> "${count / 1000}k"
        else -> count.toString()
    }
}
