# Comment Panel Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the full `:biz:story:comment-panel:platform` UI and feature contract on top of the existing comment-panel core API.

**Architecture:** Keep `:biz:story:comment-panel:core` unchanged. Update the platform feature contract first, then implement a full bottom-sheet UI where `CommentPanelSheet` owns ViewModel creation/effect collection and `CommentPanelContent` plus focused child composables render `CommentPanelState` and emit `CommentPanelEvent` callbacks.

**Tech Stack:** Kotlin, Jetpack Compose, Material3, Koin, AndroidX Compose UI tests, project MVI scaffold.

---

## File Structure

Modify these files:

- `biz/story/comment-panel/platform/feature.md`: replace the empty placeholder contract with full platform UCs.
- `biz/story/comment-panel/platform/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/CommentPanelSheet.kt`: expand from empty screen to full sheet wiring and content composables.
- `biz/story/comment-panel/platform/src/androidTest/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/CommentPanelScreenTest.kt`: replace placeholder screen test with content and sheet tests.
- `biz/story/platform/src/main/kotlin/zhaoyun/example/composedemo/story/platform/StoryCardPage.kt`: pass the dialogue callback to `CommentPanelSheet`.

Keep these files unchanged unless a compile error proves otherwise:

- `biz/story/comment-panel/core/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/core/CommentPanelState.kt`
- `biz/story/comment-panel/core/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/core/CommentPanelEvent.kt`
- `biz/story/comment-panel/core/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/core/CommentPanelEffect.kt`
- `biz/story/comment-panel/core/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/core/CommentPanelUseCase.kt`
- `biz/story/comment-panel/platform/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/CommentPanelViewModel.kt`
- `biz/story/comment-panel/platform/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/di/CommentPanelPlatformModule.kt`
- `biz/story/comment-panel/platform/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/di/CommentPanelPlatformModuleTest.kt`

---

### Task 1: Replace Platform Feature Contract

**Files:**
- Modify: `biz/story/comment-panel/platform/feature.md`

- [ ] **Step 1: Replace the placeholder contract**

Set `biz/story/comment-panel/platform/feature.md` to:

```markdown
# Feature — Comment Panel Platform（评论面板展示）

> 参考：故事评论区底部面板，覆盖评论加载状态、评论列表、回复、点赞、发送评论等平台相关展示与交互。

---

## 全局功能约束

- **业务下沉**：平台层只根据评论业务状态展示内容并转发用户操作，不重新推导复杂业务规则。
- **故事上下文**：面板打开、加载、点赞、回复和发送评论都保持当前故事上下文。
- **状态可恢复**：加载失败、分页失败、回复失败、点赞失败和发送失败不应破坏已有可用展示内容。
- **局部错误隔离**：回复失败只影响所属评论；评论分页失败只影响列表底部；输入错误只影响输入区。
- **小屏可用**：面板内容在小屏幕和键盘弹出时保持可滚动，输入区保持可见。

---

## UC-01 打开评论底部面板

**前置条件**：故事页面请求打开评论面板。  
**步骤**：

1. 页面展示评论底部面板。
2. 页面保留当前故事上下文。
3. 页面准备展示评论加载状态和输入区域。

**预期结果**：评论面板稳定打开，并与当前故事绑定。

---

## UC-02 关闭评论底部面板

**前置条件**：评论面板已打开。  
**步骤**：

1. 用户触发关闭面板。
2. 页面通知调用方关闭评论面板。
3. 页面不修改故事页面已有状态。

**预期结果**：评论面板关闭，故事页面状态保持不变。

---

## UC-03 首次展示触发评论加载

**前置条件**：评论面板首次展示。  
**步骤**：

1. 页面请求加载当前故事评论。
2. 页面展示加载中状态。

**预期结果**：评论数据开始加载；重复展示不造成重复加载或状态错乱。

---

## UC-04 首屏加载中

**前置条件**：评论首屏正在加载。  
**步骤**：

1. 页面展示加载提示。
2. 页面避免展示误导性的空评论列表。

**预期结果**：用户能识别评论正在加载。

---

## UC-05 首屏空数据

**前置条件**：评论首屏加载完成且没有评论。  
**步骤**：

1. 页面展示空评论提示。
2. 页面保留评论输入区域。

**预期结果**：用户能识别暂无评论，并可输入新评论。

---

## UC-06 首屏加载失败并重试

**前置条件**：评论首屏加载失败。  
**步骤**：

1. 页面展示错误提示和重试入口。
2. 用户点击重试。
3. 页面再次请求加载评论。

**预期结果**：用户可重新加载评论；已有可用内容不被错误状态破坏。

---

## UC-07 首屏成功展示评论内容

**前置条件**：评论首屏加载成功。  
**步骤**：

1. 页面展示评论总数。
2. 页面展示顶部对话入口状态。
3. 页面展示评论列表和输入区域。

**预期结果**：用户可浏览评论并继续互动。

---

## UC-08 顶部对话剧情入口

**前置条件**：评论面板获得顶部对话入口状态。  
**步骤**：

1. 入口可用时页面展示标题和说明。
2. 用户点击入口。
3. 页面请求外层进入对应剧情。

**预期结果**：入口可用时可触发进入剧情；入口不可用或隐藏时不占用主要内容。

---

## UC-09 展示评论基础信息

**前置条件**：评论列表有评论。  
**步骤**：

1. 页面展示用户昵称、头像占位、发布时间和正文。
2. 页面展示作者标识、置顶标识、点赞数和回复数。
3. 可选信息缺失时页面保持评论项可读。

**预期结果**：每条评论的主要信息清晰可见。

---

## UC-10 展开长评论

**前置条件**：评论支持展开且尚未展开。  
**步骤**：

1. 页面展示展开入口。
2. 用户点击展开入口。
3. 页面请求展开该评论。

**预期结果**：目标评论可展示完整正文；其它评论不受影响。

---

## UC-11 点赞和取消点赞评论

**前置条件**：评论列表中存在评论。  
**步骤**：

1. 用户点击评论点赞入口。
2. 页面展示该评论的提交中状态。
3. 页面根据业务状态展示最终点赞数和点赞状态。

**预期结果**：点赞交互只影响目标评论；提交中不会误导用户重复提交。

---

## UC-12 点赞失败提示

**前置条件**：用户已触发点赞或取消点赞。  
**步骤**：

1. 点赞请求失败。
2. 页面承接业务提示。
3. 页面按业务层最终状态展示评论。

**预期结果**：用户看到失败反馈，评论状态与业务层保持一致。

---

## UC-13 展开和收起回复

**前置条件**：评论存在回复信息。  
**步骤**：

1. 用户点击查看回复。
2. 页面展示该评论回复区域。
3. 用户点击收起。

**预期结果**：回复区域可展开和收起；已加载内容再次展开时可复用。

---

## UC-14 回复加载失败和重试

**前置条件**：某条评论回复加载失败。  
**步骤**：

1. 页面在该评论下展示回复错误。
2. 用户点击重试。
3. 页面再次请求该评论回复。

**预期结果**：失败只影响所属评论回复区域，不影响其它评论。

---

## UC-15 加载更多回复

**前置条件**：某条评论回复已展开且仍有更多回复。  
**步骤**：

1. 页面展示加载更多回复入口。
2. 用户点击加载更多。
3. 页面请求更多回复。

**预期结果**：更多回复追加到当前评论回复区；无更多时入口消失或展示结束状态。

---

## UC-16 加载更多评论

**前置条件**：评论列表已展示且仍有更多评论。  
**步骤**：

1. 页面在列表底部展示加载更多入口。
2. 用户点击加载更多。
3. 页面请求更多评论。

**预期结果**：更多评论追加到列表底部；加载失败时保留已有评论并允许重试。

---

## UC-17 评论输入和发送

**前置条件**：评论面板已打开。  
**步骤**：

1. 用户输入评论内容。
2. 页面展示当前输入。
3. 用户点击发送。

**预期结果**：有效评论可提交；发送中展示进度或禁用状态。

---

## UC-18 输入校验和发送失败

**前置条件**：用户请求发送评论。  
**步骤**：

1. 评论内容无效或发送失败。
2. 页面在输入区展示错误。
3. 页面承接业务提示。

**预期结果**：无效内容不会误认为已发送；发送失败保留用户输入。
```

- [ ] **Step 2: Review the feature file for forbidden implementation details**

Run:

```bash
rg -n "CommentPanel|ViewModel|State|Event|Effect|UseCase|Composable|testTag|Koin|字段|方法|类名" biz/story/comment-panel/platform/feature.md
```

Expected: no matches except the title path context if any. If there are matches, rewrite that sentence using user-visible behavior.

- [ ] **Step 3: Commit the feature contract**

Run:

```bash
git add biz/story/comment-panel/platform/feature.md
git commit -m "docs: declare comment panel platform feature"
```

Expected: commit succeeds.

---

### Task 2: Add Content-Level Compose Tests

**Files:**
- Modify: `biz/story/comment-panel/platform/src/androidTest/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/CommentPanelScreenTest.kt`

- [ ] **Step 1: Replace the placeholder androidTest with failing content tests**

Replace `CommentPanelScreenTest.kt` with:

```kotlin
package zhaoyun.example.composedemo.story.commentpanel.platform

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
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
    fun success_state_displays_dialogue_entry_comment_and_input() {
        setContent(successState())

        composeRule.onNodeWithText("评论 2").assertIsDisplayed()
        composeRule.onNodeWithText("进入对话剧情").assertIsDisplayed()
        composeRule.onNodeWithText("读者一号").assertIsDisplayed()
        composeRule.onNodeWithText("这个故事很有意思，想继续看后续。").assertIsDisplayed()
        composeRule.onNodeWithTag(CommentPanelTestTags.InputField).assertIsDisplayed()
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
    fun input_and_send_trigger_callbacks() {
        val inputValues = mutableListOf<String>()
        var sendCount = 0
        setContent(
            state = successState(),
            onInputChange = { inputValues += it },
            onSendClick = { sendCount++ },
        )

        composeRule.onNodeWithTag(CommentPanelTestTags.InputField).performTextInput("新评论")
        composeRule.onNodeWithText("发送").performClick()

        assertEquals(listOf("新评论"), inputValues)
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
```

- [ ] **Step 2: Run androidTest to verify tests fail**

Run:

```bash
JAVA_HOME=/Users/bytedance/Library/Java/JavaVirtualMachines/jbr-21.0.8/Contents/Home ./gradlew :biz:story:comment-panel:platform:connectedDebugAndroidTest
```

Expected: FAIL because `CommentPanelContent`, new test tags, and UI texts do not exist yet. If no device is connected, record that androidTest could not run and continue with compile checks after implementation.

- [ ] **Step 3: Commit the failing tests if the project convention allows red commits**

If following strict TDD with red commits:

```bash
git add biz/story/comment-panel/platform/src/androidTest/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/CommentPanelScreenTest.kt
git commit -m "test: cover comment panel platform states"
```

If the team avoids red commits, skip the commit and carry the test changes into Task 3.

---

### Task 3: Implement `CommentPanelContent` and Child UI

**Files:**
- Modify: `biz/story/comment-panel/platform/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/CommentPanelSheet.kt`

- [ ] **Step 1: Add imports needed by the full content UI**

Update imports in `CommentPanelSheet.kt` to include:

```kotlin
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.story.commentpanel.core.CommentItem
import zhaoyun.example.composedemo.story.commentpanel.core.CommentPanelEffect
import zhaoyun.example.composedemo.story.commentpanel.core.CommentPanelEvent
import zhaoyun.example.composedemo.story.commentpanel.core.DialogueEntryState
import zhaoyun.example.composedemo.story.commentpanel.core.LoadStatus
import zhaoyun.example.composedemo.story.commentpanel.core.ReplyItem
import zhaoyun.example.composedemo.story.commentpanel.core.ReplySectionState
```

Keep existing imports that remain used.

- [ ] **Step 2: Replace `CommentPanelScreen` body and test tags**

Use this structure in `CommentPanelSheet.kt`:

```kotlin
object CommentPanelTestTags {
    const val Loading = "comment_panel_loading"
    const val InputField = "comment_panel_input_field"
    const val LikeButton = "comment_panel_like_button"
}
```

Replace `CommentPanelScreen` with a ViewModel-backed screen that collects state and base effects:

```kotlin
@Composable
fun CommentPanelScreen(
    cardId: String,
    onDialogueRequested: (cardId: String, targetId: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val initialState = remember(cardId) {
        CommentPanelState(cardId = cardId)
    }
    MviScreen<CommentPanelViewModel>(
        parameters = { parametersOf(initialState.toStateHolder()) },
    ) { viewModel ->
        val state by viewModel.state.collectAsStateWithLifecycle()
        val context = LocalContext.current

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

        LaunchedEffect(viewModel) {
            viewModel.baseEffect.collect { effect ->
                when (effect) {
                    is BaseEffect.ShowToast ->
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    else -> Unit
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
```

- [ ] **Step 3: Implement the pure content entry**

Add below `CommentPanelScreen`:

```kotlin
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
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        CommentPanelHeader(totalCount = state.totalCount)
        Spacer(modifier = Modifier.height(12.dp))
        CommentLoadStateContent(
            state = state,
            onRetryInitialLoad = onRetryInitialLoad,
            onDialogueClick = onDialogueClick,
            onExpandComment = onExpandComment,
            onToggleLike = onToggleLike,
            onExpandReplies = onExpandReplies,
            onCollapseReplies = onCollapseReplies,
            onLoadMoreReplies = onLoadMoreReplies,
            onLoadMoreComments = onLoadMoreComments,
            modifier = Modifier.weight(1f, fill = false),
        )
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
```

- [ ] **Step 4: Implement load-state rendering**

Add:

```kotlin
@Composable
private fun CommentPanelHeader(totalCount: Int) {
    Text(
        text = "评论 $totalCount",
        style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        ),
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun CommentLoadStateContent(
    state: CommentPanelState,
    onRetryInitialLoad: () -> Unit,
    onDialogueClick: () -> Unit,
    onExpandComment: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onExpandReplies: (String) -> Unit,
    onCollapseReplies: (String) -> Unit,
    onLoadMoreReplies: (String) -> Unit,
    onLoadMoreComments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state.initialLoadStatus) {
        LoadStatus.Idle,
        LoadStatus.Loading -> LoadingState(modifier = modifier)
        LoadStatus.Empty -> EmptyState(modifier = modifier)
        LoadStatus.Error -> ErrorState(onRetry = onRetryInitialLoad, modifier = modifier)
        LoadStatus.Success -> CommentList(
            state = state,
            onDialogueClick = onDialogueClick,
            onExpandComment = onExpandComment,
            onToggleLike = onToggleLike,
            onExpandReplies = onExpandReplies,
            onCollapseReplies = onCollapseReplies,
            onLoadMoreReplies = onLoadMoreReplies,
            onLoadMoreComments = onLoadMoreComments,
            modifier = modifier,
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(modifier = Modifier.testTag(CommentPanelTestTags.Loading))
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = "正在加载评论", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "还没有评论", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "来写下第一条评论", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "评论加载失败", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRetry) {
            Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "重新加载")
        }
    }
}
```

- [ ] **Step 5: Implement list, dialogue entry, and comment row**

Add:

```kotlin
@Composable
private fun CommentList(
    state: CommentPanelState,
    onDialogueClick: () -> Unit,
    onExpandComment: (String) -> Unit,
    onToggleLike: (String) -> Unit,
    onExpandReplies: (String) -> Unit,
    onCollapseReplies: (String) -> Unit,
    onLoadMoreReplies: (String) -> Unit,
    onLoadMoreComments: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            DialogueEntryRow(entry = state.dialogueEntry, onClick = onDialogueClick)
        }
        items(state.comments, key = { it.commentId }) { comment ->
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
                pagination = state.commentPagination,
                hasComments = state.comments.isNotEmpty(),
                onLoadMoreComments = onLoadMoreComments,
            )
        }
    }
}

@Composable
private fun DialogueEntryRow(
    entry: DialogueEntryState,
    onClick: () -> Unit,
) {
    if (entry !is DialogueEntryState.Available) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
            )
        }
    }
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
    Row(modifier = Modifier.fillMaxWidth()) {
        AvatarPlaceholder(name = comment.user.nickname)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = comment.user.nickname, fontWeight = FontWeight.Bold)
                if (comment.user.isAuthor) BadgeText("作者")
                if (comment.isPinned) BadgeText("置顶")
            }
            Text(
                text = comment.content,
                maxLines = if (comment.isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (comment.canExpand && !comment.isExpanded) {
                TextButton(onClick = { onExpandComment(comment.commentId) }) {
                    Text(text = "展开")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.createdAtText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    enabled = !comment.isLikeSubmitting,
                    modifier = Modifier.testTag("${CommentPanelTestTags.LikeButton}:${comment.commentId}"),
                    onClick = { onToggleLike(comment.commentId) },
                ) {
                    Icon(
                        imageVector = if (comment.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "点赞",
                        tint = if (comment.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(text = if (comment.isLikeSubmitting) "..." else comment.likeCount.toString())
            }
            ReplySection(
                commentId = comment.commentId,
                replyCount = comment.replyCount,
                replySection = comment.replySection,
                onExpandReplies = onExpandReplies,
                onCollapseReplies = onCollapseReplies,
                onLoadMoreReplies = onLoadMoreReplies,
            )
        }
    }
}
```

- [ ] **Step 6: Implement badges, avatars, replies, pagination, and input**

Add:

```kotlin
@Composable
private fun AvatarPlaceholder(name: String) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = name.take(1), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BadgeText(text: String) {
    Spacer(modifier = Modifier.width(6.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ReplySection(
    commentId: String,
    replyCount: Int,
    replySection: ReplySectionState,
    onExpandReplies: (String) -> Unit,
    onCollapseReplies: (String) -> Unit,
    onLoadMoreReplies: (String) -> Unit,
) {
    if (!replySection.isExpanded) {
        if (replyCount > 0) {
            TextButton(onClick = { onExpandReplies(commentId) }) {
                Text(text = "查看 $replyCount 条回复")
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        replySection.replies.forEach { reply ->
            ReplyRow(reply = reply)
        }
        if (replySection.isLoading) {
            Text(text = "正在加载回复", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        replySection.errorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { onLoadMoreReplies(commentId) }) {
                Text(text = "重试加载回复")
            }
        }
        if (replySection.pagination.hasMore && !replySection.isLoading && replySection.errorMessage == null) {
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
    Column {
        Text(text = reply.user.nickname, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
        Text(text = reply.content, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CommentPaginationFooter(
    pagination: zhaoyun.example.composedemo.story.commentpanel.core.PaginationState,
    hasComments: Boolean,
    onLoadMoreComments: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            pagination.isLoading -> Text(text = "正在加载更多评论")
            pagination.errorMessage != null -> TextButton(onClick = onLoadMoreComments) {
                Text(text = "重试加载评论")
            }
            pagination.hasMore -> TextButton(onClick = onLoadMoreComments) {
                Text(text = "加载更多评论")
            }
            hasComments -> Text(text = "没有更多评论", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        inputErrorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        sendErrorMessage?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag(CommentPanelTestTags.InputField),
                placeholder = { Text(text = "写评论") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                enabled = !isSendingComment,
                onClick = onSendClick,
            ) {
                if (isSendingComment) {
                    Text(text = "发送中")
                } else {
                    Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "发送")
                }
            }
        }
    }
}
```

- [ ] **Step 7: Run androidTest or compile check**

Run if a device is available:

```bash
JAVA_HOME=/Users/bytedance/Library/Java/JavaVirtualMachines/jbr-21.0.8/Contents/Home ./gradlew :biz:story:comment-panel:platform:connectedDebugAndroidTest
```

Expected: PASS for `CommentPanelScreenTest`.

If no device is available, run:

```bash
JAVA_HOME=/Users/bytedance/Library/Java/JavaVirtualMachines/jbr-21.0.8/Contents/Home ./gradlew :biz:story:comment-panel:platform:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 8: Commit content implementation**

Run:

```bash
git add biz/story/comment-panel/platform/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/CommentPanelSheet.kt biz/story/comment-panel/platform/src/androidTest/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/CommentPanelScreenTest.kt
git commit -m "feat: render comment panel content"
```

Expected: commit succeeds.

---

### Task 4: Wire the Bottom Sheet and Story Page Callback

**Files:**
- Modify: `biz/story/comment-panel/platform/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/CommentPanelSheet.kt`
- Modify: `biz/story/platform/src/main/kotlin/zhaoyun/example/composedemo/story/platform/StoryCardPage.kt`

- [ ] **Step 1: Update `CommentPanelSheet` signature and body**

Change `CommentPanelSheet` to:

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentPanelSheet(
    cardId: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onDialogueRequested: (cardId: String, targetId: String) -> Unit = { _, _ -> },
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(),
        modifier = modifier,
    ) {
        CommentPanelScreen(
            cardId = cardId,
            onDialogueRequested = onDialogueRequested,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
```

- [ ] **Step 2: Update story page call site**

In `StoryCardPage.kt`, replace the current `CommentPanelSheet` call with:

```kotlin
CommentPanelSheet(
    cardId = commentPanelCardId.orEmpty(),
    onDismissRequest = { commentPanelCardId = null },
    onDialogueRequested = { _, _ ->
        commentPanelCardId = null
    },
)
```

This gives the effect a deterministic platform callback without adding a new story-detail destination in this task.

- [ ] **Step 3: Run platform compile**

Run:

```bash
JAVA_HOME=/Users/bytedance/Library/Java/JavaVirtualMachines/jbr-21.0.8/Contents/Home ./gradlew :biz:story:platform:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 4: Commit bottom-sheet wiring**

Run:

```bash
git add biz/story/comment-panel/platform/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/CommentPanelSheet.kt biz/story/platform/src/main/kotlin/zhaoyun/example/composedemo/story/platform/StoryCardPage.kt
git commit -m "feat: wire comment panel sheet"
```

Expected: commit succeeds.

---

### Task 5: Add a ViewModel-Backed Sheet Smoke Test

**Files:**
- Modify: `biz/story/comment-panel/platform/src/androidTest/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/CommentPanelScreenTest.kt`

- [ ] **Step 1: Add Koin imports and smoke test**

Add imports:

```kotlin
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import zhaoyun.example.composedemo.story.commentpanel.platform.di.commentPanelPlatformModule
```

Add this test to `CommentPanelScreenTest`:

```kotlin
@Test
fun sheet_loads_fake_comments_after_opening() {
    stopExistingKoin()
    startKoin {
        modules(commentPanelPlatformModule)
    }
    try {
        composeRule.setContent {
            CommentPanelScreen(cardId = "story-1")
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodesWithText("这个故事很有意思，想继续看后续。")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("这个故事很有意思，想继续看后续。").assertIsDisplayed()
    } finally {
        stopKoin()
    }
}

private fun stopExistingKoin() {
    if (GlobalContext.getOrNull() != null) {
        stopKoin()
    }
}
```

Also add:

```kotlin
import androidx.compose.ui.test.onAllNodesWithText
```

- [ ] **Step 2: Run the smoke test**

Run:

```bash
JAVA_HOME=/Users/bytedance/Library/Java/JavaVirtualMachines/jbr-21.0.8/Contents/Home ./gradlew :biz:story:comment-panel:platform:connectedDebugAndroidTest --tests zhaoyun.example.composedemo.story.commentpanel.platform.CommentPanelScreenTest.sheet_loads_fake_comments_after_opening
```

Expected: PASS if a device is connected. If Gradle does not support the `--tests` filter for connected tests in this project, run the whole connected test task instead.

- [ ] **Step 3: Commit smoke test**

Run:

```bash
git add biz/story/comment-panel/platform/src/androidTest/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/CommentPanelScreenTest.kt
git commit -m "test: cover comment panel sheet loading"
```

Expected: commit succeeds.

---

### Task 6: Final Verification and Documentation Sync Check

**Files:**
- Verify: `biz/story/comment-panel/platform/feature.md`
- Verify: `docs/superpowers/specs/2026-05-14-comment-panel-platform-design.md`
- Verify: `docs/superpowers/plans/2026-05-14-comment-panel-platform.md`

- [ ] **Step 1: Run JVM tests**

Run:

```bash
JAVA_HOME=/Users/bytedance/Library/Java/JavaVirtualMachines/jbr-21.0.8/Contents/Home ./gradlew :biz:story:comment-panel:platform:testDebugUnitTest
```

Expected: PASS.

- [ ] **Step 2: Run compile checks**

Run:

```bash
JAVA_HOME=/Users/bytedance/Library/Java/JavaVirtualMachines/jbr-21.0.8/Contents/Home ./gradlew :biz:story:comment-panel:platform:compileDebugKotlin :biz:story:platform:compileDebugKotlin
```

Expected: PASS.

- [ ] **Step 3: Run connected tests if a device is available**

Run:

```bash
adb devices
JAVA_HOME=/Users/bytedance/Library/Java/JavaVirtualMachines/jbr-21.0.8/Contents/Home ./gradlew :biz:story:comment-panel:platform:connectedDebugAndroidTest
```

Expected: `adb devices` lists at least one device and connected tests PASS. If no device is listed, record `connectedDebugAndroidTest not run: no device available`.

- [ ] **Step 4: Check feature coverage against tests**

Run:

```bash
rg -n "UC-0|UC-1" biz/story/comment-panel/platform/feature.md
rg -n "loading_state|empty_state|error_state|success_state|dialogue|comment_actions|expanded_replies|pagination|input|sheet_loads" biz/story/comment-panel/platform/src/androidTest/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/CommentPanelScreenTest.kt
```

Expected: feature UCs and test names cover loading, empty, error, success, dialogue, comments, replies, pagination, input, send, and sheet loading.

- [ ] **Step 5: Commit any final fixes**

If Step 1-4 required changes:

```bash
git add biz/story/comment-panel/platform/feature.md biz/story/comment-panel/platform/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/CommentPanelSheet.kt biz/story/comment-panel/platform/src/androidTest/kotlin/zhaoyun/example/composedemo/story/commentpanel/platform/CommentPanelScreenTest.kt biz/story/platform/src/main/kotlin/zhaoyun/example/composedemo/story/platform/StoryCardPage.kt
git commit -m "fix: finalize comment panel platform"
```

Expected: commit succeeds. If there are no final fixes, skip this commit.

---

## Self-Review Notes

- Spec coverage: Tasks cover feature contract, sheet container, first load, load states, dialogue entry, comment rows, long comment expansion, likes, replies, pagination, input, send, effect handling, story page callback, and verification.
- Placeholder scan: This plan does not use TBD/TODO/fill-in language. Optional paths are constrained to device availability or project red-commit policy.
- Type consistency: All referenced state/event/effect names match the existing `comment-panel/core` package as of the design spec.
