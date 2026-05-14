package zhaoyun.example.composedemo.story.commentpanel.platform

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.core.parameter.parametersOf
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.platform.MviScreen
import zhaoyun.example.composedemo.story.commentpanel.core.CommentItem
import zhaoyun.example.composedemo.story.commentpanel.core.CommentPanelEffect
import zhaoyun.example.composedemo.story.commentpanel.core.CommentPanelEvent
import zhaoyun.example.composedemo.story.commentpanel.core.CommentPanelState
import zhaoyun.example.composedemo.story.commentpanel.core.DialogueEntryState
import zhaoyun.example.composedemo.story.commentpanel.core.LoadStatus
import zhaoyun.example.composedemo.story.commentpanel.core.PaginationState
import zhaoyun.example.composedemo.story.commentpanel.core.ReplyItem
import zhaoyun.example.composedemo.story.commentpanel.core.ReplySectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentPanelSheet(
    cardId: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(),
        modifier = modifier,
    ) {
        CommentPanelScreen(cardId = cardId)
    }
}

@Composable
fun CommentPanelScreen(
    cardId: String,
    onDialogueRequested: (cardId: String, targetId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val initialState = remember(cardId) {
        CommentPanelState(cardId = cardId)
    }
    MviScreen<CommentPanelViewModel>(
        onBaseEffect = { effect ->
            when (effect) {
                is BaseEffect.ShowToast -> {
                    showToast(effect.message, context)
                    true
                }

                else -> false
            }
        },
        parameters = { parametersOf(initialState.toStateHolder()) },
    ) { viewModel ->
        val state by viewModel.state.collectAsStateWithLifecycle()

        LaunchedEffect(viewModel) {
            viewModel.sendEvent(CommentPanelEvent.OnPanelShown)
        }

        LaunchedEffect(viewModel) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is CommentPanelEffect.NavigateToDialogue ->
                        onDialogueRequested(effect.cardId, effect.targetId)
                }
            }
        }

        CommentPanelContent(
            state = state,
            onRetryInitialLoad = { viewModel.sendEvent(CommentPanelEvent.OnRetryInitialLoad) },
            onDialogueClick = { viewModel.sendEvent(CommentPanelEvent.OnDialogueEntryClicked) },
            onExpandComment = { viewModel.sendEvent(CommentPanelEvent.OnCommentExpanded(it)) },
            onToggleLike = { viewModel.sendEvent(CommentPanelEvent.OnCommentLikeClicked(it)) },
            onExpandReplies = { viewModel.sendEvent(CommentPanelEvent.OnRepliesExpanded(it)) },
            onCollapseReplies = { viewModel.sendEvent(CommentPanelEvent.OnRepliesCollapsed(it)) },
            onLoadMoreReplies = { viewModel.sendEvent(CommentPanelEvent.OnLoadMoreReplies(it)) },
            onLoadMoreComments = { viewModel.sendEvent(CommentPanelEvent.OnLoadMoreComments) },
            onInputChange = { viewModel.sendEvent(CommentPanelEvent.OnInputChanged(it)) },
            onSendClick = { viewModel.sendEvent(CommentPanelEvent.OnSendClicked) },
            modifier = modifier,
        )
    }
}

@Composable
internal fun CommentPanelContent(
    state: CommentPanelState,
    onRetryInitialLoad: () -> Unit,
    onDialogueClick: () -> Unit,
    onExpandComment: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onExpandReplies: (String) -> Unit,
    onCollapseReplies: (String) -> Unit,
    onLoadMoreReplies: (String) -> Unit,
    onLoadMoreComments: () -> Unit,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
    ) {
        Text(
            text = "评论 ${state.totalCount}",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = Color.Black,
        )
        Spacer(modifier = Modifier.height(12.dp))

        when (state.initialLoadStatus) {
            LoadStatus.Idle,
            LoadStatus.Loading -> LoadingState()

            LoadStatus.Empty -> EmptyState()
            LoadStatus.Error -> ErrorState(onRetryInitialLoad = onRetryInitialLoad)
            LoadStatus.Success -> SuccessState(
                state = state,
                onDialogueClick = onDialogueClick,
                onExpandComment = onExpandComment,
                onToggleLike = onToggleLike,
                onExpandReplies = onExpandReplies,
                onCollapseReplies = onCollapseReplies,
                onLoadMoreReplies = onLoadMoreReplies,
                onLoadMoreComments = onLoadMoreComments,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        CommentInputBar(
            inputText = state.inputText,
            isSendingComment = state.isSendingComment,
            inputErrorMessage = state.inputErrorMessage,
            sendErrorMessage = state.sendErrorMessage,
            onInputChange = onInputChange,
            onSendClick = onSendClick,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .testTag(CommentPanelTestTags.Loading),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "正在加载评论",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "还没有评论",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun ErrorState(onRetryInitialLoad: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "评论加载失败",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextButton(onClick = onRetryInitialLoad) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "重新加载")
        }
    }
}

@Composable
private fun SuccessState(
    state: CommentPanelState,
    onDialogueClick: () -> Unit,
    onExpandComment: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onExpandReplies: (String) -> Unit,
    onCollapseReplies: (String) -> Unit,
    onLoadMoreReplies: (String) -> Unit,
    onLoadMoreComments: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp),
    ) {
        item {
            DialogueEntry(
                dialogueEntry = state.dialogueEntry,
                onDialogueClick = onDialogueClick,
            )
        }

        items(
            items = state.comments,
            key = { it.commentId },
        ) { comment ->
            CommentRow(
                comment = comment,
                onExpandComment = onExpandComment,
                onToggleLike = onToggleLike,
                onExpandReplies = onExpandReplies,
                onCollapseReplies = onCollapseReplies,
                onLoadMoreReplies = onLoadMoreReplies,
            )
        }

        item {
            CommentPaginationFooter(
                comments = state.comments,
                pagination = state.commentPagination,
                onLoadMoreComments = onLoadMoreComments,
            )
        }
    }
}

@Composable
private fun DialogueEntry(
    dialogueEntry: DialogueEntryState,
    onDialogueClick: () -> Unit,
) {
    val entry = dialogueEntry as? DialogueEntryState.Available ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF6F6F6))
            .clickable(onClick = onDialogueClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE9EEF8)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = null,
                tint = Color(0xFF4169E1),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.Black,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = Color.Black.copy(alpha = 0.58f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun CommentRow(
    comment: CommentItem,
    onExpandComment: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onExpandReplies: (String) -> Unit,
    onCollapseReplies: (String) -> Unit,
    onLoadMoreReplies: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = comment.user.nickname,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.Black,
            )
            if (comment.user.isAuthor) {
                Spacer(modifier = Modifier.width(6.dp))
                LabelPill(text = "作者")
            }
            if (comment.isPinned) {
                Spacer(modifier = Modifier.width(6.dp))
                LabelPill(text = "置顶")
            }
            Spacer(modifier = Modifier.weight(1f))
            LikeButton(
                comment = comment,
                onToggleLike = onToggleLike,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = comment.content,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Black.copy(alpha = 0.82f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = comment.createdAtText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = Color.Black.copy(alpha = 0.46f),
            )
            if (comment.canExpand && !comment.isExpanded) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = { onExpandComment(comment.commentId) },
                    modifier = Modifier.height(32.dp),
                ) {
                    Text(text = "展开")
                }
            }
        }

        if (!comment.replySection.isExpanded && comment.replyCount > 0) {
            TextButton(onClick = { onExpandReplies(comment.commentId) }) {
                Text(text = "查看 ${comment.replyCount} 条回复")
            }
        }

        ReplySection(
            commentId = comment.commentId,
            replySection = comment.replySection,
            onCollapseReplies = onCollapseReplies,
            onLoadMoreReplies = onLoadMoreReplies,
        )
    }
}

@Composable
private fun LabelPill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = Color(0xFF4169E1),
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFE9EEF8))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun LikeButton(
    comment: CommentItem,
    onToggleLike: (String) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            enabled = !comment.isLikeSubmitting,
            onClick = { onToggleLike(comment.commentId) },
            modifier = Modifier
                .size(36.dp)
                .testTag("${CommentPanelTestTags.LikeButton}:${comment.commentId}"),
        ) {
            if (comment.isLikeSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = if (comment.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "点赞",
                    tint = if (comment.isLiked) Color(0xFFFF6B6B) else Color.Black.copy(alpha = 0.62f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            text = comment.likeCount.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = Color.Black.copy(alpha = 0.55f),
        )
    }
}

@Composable
private fun ReplySection(
    commentId: String,
    replySection: ReplySectionState,
    onCollapseReplies: (String) -> Unit,
    onLoadMoreReplies: (String) -> Unit,
) {
    if (!replySection.isExpanded) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 6.dp),
    ) {
        replySection.replies.forEach { reply ->
            ReplyRow(reply = reply)
        }

        if (replySection.isLoading) {
            Text(
                text = "正在加载回复",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }

        replySection.errorMessage?.let { errorMessage ->
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = { onLoadMoreReplies(commentId) }) {
                Text(text = "重试加载回复")
            }
        }

        if (
            replySection.pagination.hasMore &&
            !replySection.isLoading &&
            replySection.errorMessage == null
        ) {
            TextButton(onClick = { onLoadMoreReplies(commentId) }) {
                Text(text = "加载更多回复")
            }
        }

        TextButton(onClick = { onCollapseReplies(commentId) }) {
            Text(text = "收起回复")
        }
    }
}

@Composable
private fun ReplyRow(reply: ReplyItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = reply.user.nickname,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.Black.copy(alpha = 0.72f),
            )
            if (reply.user.isAuthor) {
                Spacer(modifier = Modifier.width(6.dp))
                LabelPill(text = "作者")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = reply.createdAtText,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = Color.Black.copy(alpha = 0.42f),
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = reply.content,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black.copy(alpha = 0.74f),
        )
    }
}

@Composable
private fun CommentPaginationFooter(
    comments: List<CommentItem>,
    pagination: PaginationState,
    onLoadMoreComments: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            pagination.isLoading -> Text(
                text = "正在加载更多评论",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black.copy(alpha = 0.55f),
            )

            pagination.errorMessage != null -> TextButton(onClick = onLoadMoreComments) {
                Text(text = "重试加载评论")
            }

            pagination.hasMore -> TextButton(onClick = onLoadMoreComments) {
                Text(text = "加载更多评论")
            }

            comments.isNotEmpty() -> Text(
                text = "没有更多评论",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Black.copy(alpha = 0.42f),
            )
        }
    }
}

@Composable
private fun CommentInputBar(
    inputText: String,
    isSendingComment: Boolean,
    inputErrorMessage: String?,
    sendErrorMessage: String?,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag(CommentPanelTestTags.InputField),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                placeholder = { Text(text = "写下你的评论") },
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = onSendClick,
                enabled = !isSendingComment,
            ) {
                Text(text = if (isSendingComment) "发送中" else "发送")
            }
        }

        inputErrorMessage?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.error,
            )
        }
        sendErrorMessage?.let {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

object CommentPanelTestTags {
    const val EmptyScreen = "comment_panel_empty_screen"
    const val Loading = "comment_panel_loading"
    const val InputField = "comment_panel_input_field"
    const val LikeButton = "comment_panel_like_button"
}

private fun showToast(message: String, context: android.content.Context) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
