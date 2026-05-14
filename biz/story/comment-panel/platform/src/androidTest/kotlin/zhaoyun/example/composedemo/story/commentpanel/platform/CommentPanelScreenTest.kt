package zhaoyun.example.composedemo.story.commentpanel.platform

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import zhaoyun.example.composedemo.story.commentpanel.core.CommentItem
import zhaoyun.example.composedemo.story.commentpanel.core.CommentPanelState
import zhaoyun.example.composedemo.story.commentpanel.core.CommentUser
import zhaoyun.example.composedemo.story.commentpanel.core.DialogueEntryState
import zhaoyun.example.composedemo.story.commentpanel.core.LoadStatus
import zhaoyun.example.composedemo.story.commentpanel.core.PaginationState
import zhaoyun.example.composedemo.story.commentpanel.core.ReplyItem
import zhaoyun.example.composedemo.story.commentpanel.core.ReplySectionState

class CommentPanelScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loading_state_is_displayed() {
        setContent(CommentPanelState(initialLoadStatus = LoadStatus.Loading))

        composeRule.onNodeWithTag(CommentPanelTestTags.Loading).assertIsDisplayed()
    }

    @Test
    fun empty_state_keeps_input_visible() {
        setContent(CommentPanelState(initialLoadStatus = LoadStatus.Empty))

        composeRule.onNodeWithText("还没有评论").assertIsDisplayed()
        composeRule.onNodeWithTag(CommentPanelTestTags.InputField).assertIsDisplayed()
    }

    @Test
    fun error_state_retry_triggers_callback() {
        var retryCount = 0
        setContent(
            state = CommentPanelState(initialLoadStatus = LoadStatus.Error),
            onRetryInitialLoad = { retryCount++ },
        )

        composeRule.onNodeWithText("重新加载").performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun error_state_with_existing_comments_keeps_comments_and_retry() {
        var retryCount = 0
        setContent(
            state = successState().copy(initialLoadStatus = LoadStatus.Error),
            onRetryInitialLoad = { retryCount++ },
        )

        composeRule.onNodeWithText("评论刷新失败").assertIsDisplayed()
        composeRule.onNodeWithText("这个故事很有意思，想继续看后续。").assertIsDisplayed()
        composeRule.onNodeWithText("重新加载").performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun success_state_displays_dialogue_entry_comment_and_input() {
        setContent(successState())

        composeRule.onNodeWithText("评论 2").assertIsDisplayed()
        composeRule.onNodeWithText("进入对话剧情").assertIsDisplayed()
        composeRule.onNodeWithText("读者一号").assertIsDisplayed()
        composeRule.onNodeWithText("这个故事很有意思，想继续看后续。").assertIsDisplayed()
        composeRule.onNodeWithTag(CommentPanelTestTags.InputField).assertIsDisplayed()
    }

    @Test
    fun comment_rows_display_avatar_placeholder() {
        setContent(successState())

        composeRule.onNodeWithText("读").assertIsDisplayed()
    }

    @Test
    fun dialogue_entry_click_triggers_callback() {
        var clicks = 0
        setContent(successState(), onDialogueClick = { clicks++ })

        composeRule.onNodeWithText("进入对话剧情").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun comment_actions_trigger_callbacks() {
        val events = mutableListOf<String>()
        setContent(
            state = successState(),
            onExpandComment = { events += "expand:$it" },
            onToggleLike = { events += "like:$it" },
            onExpandReplies = { events += "replies:$it" },
        )

        composeRule.onNodeWithText("展开").performClick()
        composeRule.onNodeWithTag("${CommentPanelTestTags.LikeButton}:comment-1").performClick()
        composeRule.onNodeWithText("查看 2 条回复").performClick()

        assertEquals(listOf("expand:comment-1", "like:comment-1", "replies:comment-1"), events)
    }

    @Test
    fun expanded_replies_display_reply_actions() {
        val events = mutableListOf<String>()
        setContent(
            state = successState(
                firstReplySection = ReplySectionState(
                    isExpanded = true,
                    replies = listOf(
                        ReplyItem(
                            replyId = "reply-1",
                            parentCommentId = "comment-1",
                            user = CommentUser("author-1", "作者小云", isAuthor = true),
                            content = "谢谢喜欢。",
                            createdAtText = "刚刚",
                        ),
                    ),
                    pagination = PaginationState(nextCursor = "cursor-1", hasMore = true),
                ),
            ),
            onCollapseReplies = { events += "collapse:$it" },
            onLoadMoreReplies = { events += "moreReplies:$it" },
        )

        composeRule.onNodeWithText("谢谢喜欢。").assertIsDisplayed()
        composeRule.onNodeWithText("加载更多回复").performClick()
        composeRule.onNodeWithText("收起回复").performClick()

        assertEquals(listOf("moreReplies:comment-1", "collapse:comment-1"), events)
    }

    @Test
    fun reply_pagination_error_retry_triggers_load_more_replies() {
        val events = mutableListOf<String>()
        setContent(
            state = successState(
                firstReplySection = ReplySectionState(
                    isExpanded = true,
                    errorMessage = "回复加载失败",
                    pagination = PaginationState(nextCursor = "reply-cursor", hasMore = true),
                ),
            ),
            onLoadMoreReplies = { events += "moreReplies:$it" },
        )

        composeRule.onNodeWithText("回复加载失败").assertIsDisplayed()
        composeRule.onNodeWithText("重试加载回复").performClick()

        assertEquals(listOf("moreReplies:comment-1"), events)
    }

    @Test
    fun pagination_error_retry_triggers_load_more_comments() {
        var loadMoreCount = 0
        setContent(
            state = successState(
                commentPagination = PaginationState(
                    nextCursor = "cursor-2",
                    hasMore = true,
                    errorMessage = "评论加载失败",
                ),
            ),
            onLoadMoreComments = { loadMoreCount++ },
        )

        composeRule.onNodeWithText("重试加载评论").performClick()

        assertEquals(1, loadMoreCount)
    }

    @Test
    fun input_change_triggers_callback_without_local_state_assumption() {
        val inputValues = mutableListOf<String>()
        setContent(
            state = successState(),
            onInputChange = { inputValues += it },
        )

        composeRule.onNodeWithTag(CommentPanelTestTags.InputField).performTextInput("新评论")

        assertEquals(listOf("新评论"), inputValues)
    }

    @Test
    fun send_click_triggers_callback_for_controlled_input_text() {
        var sendCount = 0
        setContent(
            state = successState(inputText = "新评论"),
            onSendClick = { sendCount++ },
        )

        composeRule.onNodeWithText("新评论").assertIsDisplayed()
        composeRule.onNodeWithText("发送").performClick()

        assertEquals(1, sendCount)
    }

    @Test
    fun sending_and_error_states_are_visible() {
        setContent(
            successState(
                inputText = "旧评论",
                isSendingComment = true,
                inputErrorMessage = "请输入评论内容",
                sendErrorMessage = "发送失败，请重试",
            ),
        )

        composeRule.onNodeWithText("旧评论").assertIsDisplayed()
        composeRule.onNodeWithText("发送中").assertIsDisplayed()
        composeRule.onNodeWithText("请输入评论内容").assertIsDisplayed()
        composeRule.onNodeWithText("发送失败，请重试").assertIsDisplayed()
    }

    private fun setContent(
        state: CommentPanelState,
        onRetryInitialLoad: () -> Unit = {},
        onDialogueClick: () -> Unit = {},
        onExpandComment: (String) -> Unit = {},
        onToggleLike: (String) -> Unit = {},
        onExpandReplies: (String) -> Unit = {},
        onCollapseReplies: (String) -> Unit = {},
        onLoadMoreReplies: (String) -> Unit = {},
        onLoadMoreComments: () -> Unit = {},
        onInputChange: (String) -> Unit = {},
        onSendClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            CommentPanelContent(
                state = state,
                onRetryInitialLoad = onRetryInitialLoad,
                onDialogueClick = onDialogueClick,
                onExpandComment = onExpandComment,
                onToggleLike = onToggleLike,
                onExpandReplies = onExpandReplies,
                onCollapseReplies = onCollapseReplies,
                onLoadMoreReplies = onLoadMoreReplies,
                onLoadMoreComments = onLoadMoreComments,
                onInputChange = onInputChange,
                onSendClick = onSendClick,
            )
        }
    }

    private fun successState(
        firstReplySection: ReplySectionState = ReplySectionState(),
        commentPagination: PaginationState = PaginationState(),
        inputText: String = "",
        isSendingComment: Boolean = false,
        inputErrorMessage: String? = null,
        sendErrorMessage: String? = null,
    ): CommentPanelState {
        val reader = CommentUser("user-1", "读者一号")
        val author = CommentUser("author-1", "作者小云", isAuthor = true)
        return CommentPanelState(
            cardId = "story-1",
            totalCount = 2,
            dialogueEntry = DialogueEntryState.Available(
                title = "进入对话剧情",
                description = "和角色继续聊下去",
                targetId = "dialogue-1",
            ),
            comments = listOf(
                CommentItem(
                    commentId = "comment-1",
                    user = reader,
                    content = "这个故事很有意思，想继续看后续。",
                    createdAtText = "刚刚",
                    likeCount = 12,
                    isPinned = true,
                    canExpand = true,
                    replyCount = 2,
                    replySection = firstReplySection,
                ),
                CommentItem(
                    commentId = "comment-2",
                    user = author,
                    content = "后续剧情会继续更新。",
                    createdAtText = "1分钟前",
                    likeCount = 3,
                    isLiked = true,
                    replyCount = 0,
                ),
            ),
            initialLoadStatus = LoadStatus.Success,
            commentPagination = commentPagination,
            inputText = inputText,
            isSendingComment = isSendingComment,
            inputErrorMessage = inputErrorMessage,
            sendErrorMessage = sendErrorMessage,
        )
    }
}
