package zhaoyun.example.composedemo.story.infobar.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarEvent

@Composable
fun InfoBarArea(
    viewModel: InfoBarViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.padding(top = 12.dp),
    ) {
        // 第一行：作者信息 + 横向排列的图标按钮
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧：作者信息
            Column(
                modifier = Modifier.weight(1f),
            ) {
                // 作者名（大字号白色）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = state.creatorName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = Color.White,
                    )
                    if (state.creatorHandle.isNotEmpty()) {
                        Text(
                            text = " (${state.creatorHandle})",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                            ),
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = ">",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 简介（小字号灰色）
                Text(
                    text = "@${state.creatorHandle}（喜欢摆烂）",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                    ),
                    color = Color.White.copy(alpha = 0.6f),
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 右侧：横向排列的图标按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // 点赞
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
                    onClick = { viewModel.onEvent(InfoBarEvent.OnLikeClicked) },
                )

                // 分享
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
                    onClick = { viewModel.onEvent(InfoBarEvent.OnShareClicked) },
                )

                // 评论
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
                    onClick = { viewModel.onEvent(InfoBarEvent.OnCommentClicked) },
                )

                // 历史
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { viewModel.onEvent(InfoBarEvent.OnHistoryClicked) },
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
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                        ),
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
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
