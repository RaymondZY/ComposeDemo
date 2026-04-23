package zhaoyun.example.composedemo.story.infobar.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarEvent

@Composable
fun InfoBarArea(
    viewModel: InfoBarViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Row(
        modifier = modifier.padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = state.storyTitle,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "${state.creatorName} @${state.creatorHandle}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { viewModel.onEvent(InfoBarEvent.OnLikeClicked) }) {
                Text(
                    text = "${if (state.isLiked) "❤️" else "🤍"} ${state.likes}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            IconButton(onClick = { viewModel.onEvent(InfoBarEvent.OnShareClicked) }) {
                Text(
                    text = "↗️ ${state.shares}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            IconButton(onClick = { viewModel.onEvent(InfoBarEvent.OnCommentClicked) }) {
                Text(
                    text = "💬 ${state.comments}",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            IconButton(onClick = { viewModel.onEvent(InfoBarEvent.OnHistoryClicked) }) {
                Text(
                    text = "🕐",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
