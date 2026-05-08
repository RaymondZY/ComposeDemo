package zhaoyun.example.composedemo.story.commentpanel.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.core.parameter.parametersOf
import zhaoyun.example.composedemo.scaffold.android.screenViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.story.commentpanel.domain.CommentItem
import zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelEvent
import zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentPanelSheet(
    cardId: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialState = remember(cardId) {
        CommentPanelState(cardId = cardId)
    }
    val viewModel: CommentPanelViewModel = screenViewModel(cardId) {
        parametersOf(cardId, initialState.toStateHolder())
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.sendEvent(CommentPanelEvent.OnPanelShown)
    }

    LaunchedEffect(viewModel) {
        viewModel.baseEffect.collect { effect ->
            when (effect) {
                is BaseEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                else -> Unit
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(),
        modifier = modifier,
    ) {
        CommentPanelContent(
            state = state,
            onInputChanged = { viewModel.sendEvent(CommentPanelEvent.OnInputChanged(it)) },
            onSendClicked = { viewModel.sendEvent(CommentPanelEvent.OnSendClicked) },
        )
    }
}

@Composable
internal fun CommentPanelContent(
    state: CommentPanelState,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
    ) {
        Text(
            text = "评论",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(modifier = Modifier.height(12.dp))

        when {
            state.isLoadingComments -> {
                Text(
                    text = "评论加载中",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            state.comments.isEmpty() -> {
                Text(
                    text = "暂无评论",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 260.dp),
                ) {
                    items(state.comments) { comment ->
                        CommentRow(comment = comment)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = state.inputText,
                onValueChange = onInputChanged,
                placeholder = { Text("写下你的评论") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onSendClicked,
                enabled = !state.isSendingComment,
            ) {
                Text("发送")
            }
        }
    }
}

@Composable
private fun CommentRow(comment: CommentItem) {
    Column {
        Text(
            text = comment.authorName,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = comment.content,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
