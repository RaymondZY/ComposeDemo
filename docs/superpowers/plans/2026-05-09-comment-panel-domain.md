# 评论面板领域层实现计划

> **给执行代理：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务执行。步骤使用 勾选框（`- [ ]`）跟踪。

**目标：** 实现 `:biz:story:comment-panel:domain` 的完整评论面板业务能力，覆盖 `feature.md` 中 UC-01 到 UC-18。

**架构：** 业务规则全部放在 `biz/story/comment-panel/domain`，presentation 继续保留空 `MviScreen`，后续只消费 domain state 并发送 event。Domain state 保持不可变；数据访问通过 `CommentRepository` 抽象；`FakeCommentRepository` 提供稳定、可测试的 mock 数据。

**技术栈：** Kotlin、Kotlin Coroutines、自定义 MVI scaffold、JUnit、kotlinx-coroutines-test。

---

## 文件职责

| 文件 | 职责 |
|------|------|
| `CommentPanelState.kt` | 定义面板 State、评论、回复、分页、用户、剧情入口等领域模型 |
| `CommentPanelEvent.kt` | 定义 UC-01 到 UC-18 所需的输入事件 |
| `CommentPanelEffect.kt` | 定义一次性领域效果 |
| `CommentRepository.kt` | 定义评论数据源接口及返回模型 |
| `FakeCommentRepository.kt` | 提供稳定 fake 数据源，供本地开发和单测使用 |
| `CommentPanelUseCase.kt` | 实现加载、分页、剧情入口、点赞、回复、输入校验和发送评论 |
| `CommentPanelUseCaseTest.kt` | 纯 JVM 单测覆盖全部业务路径 |
| `feature.md` | 仅在实现命名和需求文档不一致时做同步修正 |

---

## 任务 1：定义 Domain 模型、事件、效果和 Repository 合约

**文件：**
- 修改：`biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt`
- 修改：`biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelState.kt`
- 修改：`biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelEvent.kt`
- 修改：`biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelEffect.kt`
- 新增：`biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentRepository.kt`

- [ ] **步骤 1：先写失败的模型契约测试**

将 `CommentPanelUseCaseTest.kt` 替换为以下测试：

```kotlin
package zhaoyun.example.composedemo.story.commentpanel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentPanelUseCaseTest {
    @Test
    fun `初始状态表达可加载的空评论面板`() {
        val state = CommentPanelState(cardId = "story-1")

        assertEquals("story-1", state.cardId)
        assertEquals(0, state.totalCount)
        assertEquals(LoadStatus.Idle, state.initialLoadStatus)
        assertEquals(DialogueEntryState.Hidden, state.dialogueEntry)
        assertEquals(emptyList<CommentItem>(), state.comments)
        assertFalse(state.commentPagination.hasMore)
        assertEquals("", state.inputText)
        assertFalse(state.isSendingComment)
        assertEquals(null, state.inputErrorMessage)
        assertEquals(null, state.sendErrorMessage)
    }

    @Test
    fun `评论模型包含用户点赞展开和回复状态`() {
        val user = CommentUser(
            userId = "user-1",
            nickname = "小云",
            avatarUrl = "https://example.com/u.png",
            isAuthor = true,
        )
        val comment = CommentItem(
            commentId = "comment-1",
            user = user,
            content = "这是一条评论",
            createdAtText = "刚刚",
            likeCount = 3,
            isLiked = false,
            isPinned = true,
            canExpand = true,
            replyCount = 2,
        )

        assertEquals("comment-1", comment.commentId)
        assertEquals(user, comment.user)
        assertFalse(comment.isLikeSubmitting)
        assertFalse(comment.isExpanded)
        assertFalse(comment.replySection.isExpanded)
        assertEquals(emptyList<ReplyItem>(), comment.replySection.replies)
        assertTrue(comment.canExpand)
    }
}
```

- [ ] **步骤 2：运行测试确认红灯**

运行：

```bash
./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest
```

预期：编译失败，提示 `LoadStatus`、`DialogueEntryState`、`CommentItem`、`CommentUser`、`ReplyItem` 等未定义。

- [ ] **步骤 3：实现 `CommentPanelState.kt`**

将 `CommentPanelState.kt` 替换为完整状态模型：

```kotlin
package zhaoyun.example.composedemo.story.commentpanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

private const val DefaultPageSize = 20
const val CommentPanelMaxInputLength = 200
const val CommentPanelInitialPageSize = DefaultPageSize
const val CommentPanelReplyPageSize = 10

data class CommentPanelState(
    val cardId: String = "",
    val totalCount: Int = 0,
    val dialogueEntry: DialogueEntryState = DialogueEntryState.Hidden,
    val comments: List<CommentItem> = emptyList(),
    val initialLoadStatus: LoadStatus = LoadStatus.Idle,
    val commentPagination: PaginationState = PaginationState(),
    val inputText: String = "",
    val isSendingComment: Boolean = false,
    val inputErrorMessage: String? = null,
    val sendErrorMessage: String? = null,
) : UiState

enum class LoadStatus {
    Idle,
    Loading,
    Success,
    Empty,
    Error,
}

data class PaginationState(
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed class DialogueEntryState {
    data object Hidden : DialogueEntryState()
    data class Available(
        val title: String,
        val description: String,
        val targetId: String,
    ) : DialogueEntryState()
    data class Unavailable(
        val reason: String,
    ) : DialogueEntryState()
}

data class CommentUser(
    val userId: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val isAuthor: Boolean = false,
)

data class CommentItem(
    val commentId: String,
    val user: CommentUser,
    val content: String,
    val createdAtText: String,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val isLikeSubmitting: Boolean = false,
    val isPinned: Boolean = false,
    val canExpand: Boolean = false,
    val isExpanded: Boolean = false,
    val replyCount: Int = 0,
    val replySection: ReplySectionState = ReplySectionState(),
)

data class ReplySectionState(
    val isExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val replies: List<ReplyItem> = emptyList(),
    val pagination: PaginationState = PaginationState(),
    val errorMessage: String? = null,
)

data class ReplyItem(
    val replyId: String,
    val parentCommentId: String,
    val user: CommentUser,
    val content: String,
    val createdAtText: String,
)
```

- [ ] **步骤 4：实现 `CommentPanelEvent.kt`**

```kotlin
package zhaoyun.example.composedemo.story.commentpanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class CommentPanelEvent : UiEvent {
    data object OnPanelShown : CommentPanelEvent()
    data object OnRetryInitialLoad : CommentPanelEvent()
    data object OnLoadMoreComments : CommentPanelEvent()
    data object OnDialogueEntryClicked : CommentPanelEvent()
    data class OnCommentExpanded(val commentId: String) : CommentPanelEvent()
    data class OnCommentLikeClicked(val commentId: String) : CommentPanelEvent()
    data class OnRepliesExpanded(val commentId: String) : CommentPanelEvent()
    data class OnRepliesCollapsed(val commentId: String) : CommentPanelEvent()
    data class OnLoadMoreReplies(val commentId: String) : CommentPanelEvent()
    data class OnInputChanged(val text: String) : CommentPanelEvent()
    data object OnSendClicked : CommentPanelEvent()
}
```

- [ ] **步骤 5：实现 `CommentPanelEffect.kt`**

```kotlin
package zhaoyun.example.composedemo.story.commentpanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect

sealed class CommentPanelEffect : UiEffect {
    data class NavigateToDialogue(
        val cardId: String,
        val targetId: String,
    ) : CommentPanelEffect()
}
```

- [ ] **步骤 6：新增 `CommentRepository.kt`**

```kotlin
package zhaoyun.example.composedemo.story.commentpanel.domain

interface CommentRepository {
    suspend fun loadInitial(cardId: String, pageSize: Int): CommentInitialResult
    suspend fun loadMoreComments(cardId: String, cursor: String, pageSize: Int): CommentPage
    suspend fun loadReplies(cardId: String, commentId: String, cursor: String?, pageSize: Int): ReplyPage
    suspend fun setCommentLiked(cardId: String, commentId: String, liked: Boolean): CommentLikeResult
    suspend fun sendComment(cardId: String, content: String): SendCommentResult
}

data class CommentInitialResult(
    val totalCount: Int,
    val dialogueEntry: DialogueEntryState,
    val page: CommentPage,
)

data class CommentPage(
    val comments: List<CommentItem>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

data class ReplyPage(
    val replies: List<ReplyItem>,
    val nextCursor: String?,
    val hasMore: Boolean,
)

data class CommentLikeResult(
    val commentId: String,
    val isLiked: Boolean,
    val likeCount: Int,
)

data class SendCommentResult(
    val comment: CommentItem,
    val totalCount: Int,
)
```

- [ ] **步骤 7：运行测试确认绿灯**

运行：

```bash
./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest
```

预期：`BUILD SUCCESSFUL`。

- [ ] **步骤 8：提交任务 1**

```bash
git add biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelState.kt biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelEvent.kt biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelEffect.kt biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentRepository.kt biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt
git commit -m "feat(comment-panel): define domain contract"
```

---

## 任务 2：新增稳定 Fake Repository

**文件：**
- 新增：`biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/FakeCommentRepository.kt`
- 修改：`biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt`

- [ ] **步骤 1：添加失败的 fake repository 测试**

在 `CommentPanelUseCaseTest` 追加：

```kotlin
@Test
fun `fake repository returns dialogue entry and first comment page`() = kotlinx.coroutines.test.runTest {
    val repository = FakeCommentRepository()

    val result = repository.loadInitial(cardId = "story-1", pageSize = 2)

    assertEquals(5, result.totalCount)
    assertTrue(result.dialogueEntry is DialogueEntryState.Available)
    assertEquals(2, result.page.comments.size)
    assertEquals("comment-1", result.page.comments.first().commentId)
    assertEquals("cursor-2", result.page.nextCursor)
    assertTrue(result.page.hasMore)
}

@Test
fun `fake repository supports comments replies like and send`() = kotlinx.coroutines.test.runTest {
    val repository = FakeCommentRepository()

    val moreComments = repository.loadMoreComments(cardId = "story-1", cursor = "cursor-2", pageSize = 2)
    val replies = repository.loadReplies(cardId = "story-1", commentId = "comment-1", cursor = null, pageSize = 1)
    val like = repository.setCommentLiked(cardId = "story-1", commentId = "comment-1", liked = true)
    val send = repository.sendComment(cardId = "story-1", content = "新评论")

    assertEquals(listOf("comment-3", "comment-4"), moreComments.comments.map { it.commentId })
    assertEquals(1, replies.replies.size)
    assertEquals("reply-1", replies.replies.first().replyId)
    assertEquals(CommentLikeResult("comment-1", isLiked = true, likeCount = 13), like)
    assertEquals("新评论", send.comment.content)
    assertEquals(6, send.totalCount)
}
```

- [ ] **步骤 2：运行测试确认红灯**

```bash
./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest
```

预期：失败，提示 `FakeCommentRepository` 未定义。

- [ ] **步骤 3：实现 `FakeCommentRepository.kt`**

实现稳定数据池：

```kotlin
package zhaoyun.example.composedemo.story.commentpanel.domain

class FakeCommentRepository : CommentRepository {
    private val users = listOf(
        CommentUser("author-1", "作者小云", "https://example.com/a.png", isAuthor = true),
        CommentUser("user-1", "读者一号", "https://example.com/u1.png"),
        CommentUser("user-2", "读者二号", "https://example.com/u2.png"),
    )

    private val comments = listOf(
        createComment("comment-1", users[1], "这个故事很有意思，想继续看后续。", 12, replyCount = 3, isPinned = true),
        createComment("comment-2", users[2], "这个角色的台词很有画面感。", 5, replyCount = 0),
        createComment("comment-3", users[0], "后续剧情会继续更新。", 21, replyCount = 1),
        createComment("comment-4", users[1], "这一段对话可以展开讲讲。", 1, replyCount = 0),
        createComment("comment-5", users[2], "期待更多互动剧情。", 7, replyCount = 2),
    )

    private val replies = mapOf(
        "comment-1" to listOf(
            ReplyItem("reply-1", "comment-1", users[0], "谢谢喜欢。", "刚刚"),
            ReplyItem("reply-2", "comment-1", users[2], "我也想看后续。", "1分钟前"),
            ReplyItem("reply-3", "comment-1", users[1], "已经收藏了。", "2分钟前"),
        ),
        "comment-3" to listOf(
            ReplyItem("reply-4", "comment-3", users[1], "坐等更新。", "3分钟前"),
        ),
        "comment-5" to listOf(
            ReplyItem("reply-5", "comment-5", users[0], "会加互动。", "5分钟前"),
            ReplyItem("reply-6", "comment-5", users[2], "太好了。", "6分钟前"),
        ),
    )

    override suspend fun loadInitial(cardId: String, pageSize: Int): CommentInitialResult {
        return CommentInitialResult(
            totalCount = comments.size,
            dialogueEntry = DialogueEntryState.Available(
                title = "进入对话剧情",
                description = "和角色继续聊下去",
                targetId = "$cardId-dialogue",
            ),
            page = page(comments, start = 0, pageSize = pageSize),
        )
    }

    override suspend fun loadMoreComments(cardId: String, cursor: String, pageSize: Int): CommentPage {
        return page(comments, start = cursor.removePrefix("cursor-").toInt(), pageSize = pageSize)
    }

    override suspend fun loadReplies(cardId: String, commentId: String, cursor: String?, pageSize: Int): ReplyPage {
        val allReplies = replies[commentId].orEmpty()
        val start = cursor?.removePrefix("cursor-")?.toInt() ?: 0
        val end = minOf(start + pageSize, allReplies.size)
        return ReplyPage(
            replies = allReplies.subList(start, end),
            nextCursor = if (end < allReplies.size) "cursor-$end" else null,
            hasMore = end < allReplies.size,
        )
    }

    override suspend fun setCommentLiked(cardId: String, commentId: String, liked: Boolean): CommentLikeResult {
        val comment = comments.first { it.commentId == commentId }
        val likeCount = if (liked) comment.likeCount + 1 else (comment.likeCount - 1).coerceAtLeast(0)
        return CommentLikeResult(commentId, liked, likeCount)
    }

    override suspend fun sendComment(cardId: String, content: String): SendCommentResult {
        return SendCommentResult(
            comment = createComment("local-$cardId-$content", users[0], content, likeCount = 0, replyCount = 0),
            totalCount = comments.size + 1,
        )
    }

    private fun createComment(
        id: String,
        user: CommentUser,
        content: String,
        likeCount: Int,
        replyCount: Int,
        isPinned: Boolean = false,
    ): CommentItem = CommentItem(
        commentId = id,
        user = user,
        content = content,
        createdAtText = "刚刚",
        likeCount = likeCount,
        isPinned = isPinned,
        canExpand = content.length > 18,
        replyCount = replyCount,
    )

    private fun page(items: List<CommentItem>, start: Int, pageSize: Int): CommentPage {
        val end = minOf(start + pageSize, items.size)
        return CommentPage(
            comments = items.subList(start, end),
            nextCursor = if (end < items.size) "cursor-$end" else null,
            hasMore = end < items.size,
        )
    }
}
```

- [ ] **步骤 4：运行测试确认绿灯**

```bash
./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest
```

预期：`BUILD SUCCESSFUL`。

- [ ] **步骤 5：提交任务 2**

```bash
git add biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/FakeCommentRepository.kt biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt
git commit -m "feat(comment-panel): add fake comment repository"
```

---

## 任务 3：实现首屏加载、重试、剧情入口和评论分页

**文件：**
- 修改：`CommentPanelUseCaseTest.kt`
- 修改：`CommentPanelUseCase.kt`

- [ ] **步骤 1：添加首屏加载与分页红灯测试**

追加以下测试：

```kotlin
@Test
fun `panel shown loads first page and dialogue entry`() = runTest

@Test
fun `initial load empty page enters empty state`() = runTest

@Test
fun `initial load failure keeps existing comments`() = runTest

@Test
fun `dialogue entry click emits navigation when available`() = runTest

@Test
fun `load more comments appends and preserves existing comments on failure`() = runTest
```

测试断言必须覆盖：
- `OnPanelShown` 后 `initialLoadStatus == LoadStatus.Success`
- 成功加载 `totalCount == 5`
- 剧情入口为 `DialogueEntryState.Available`
- 空首屏进入 `LoadStatus.Empty`
- 首屏失败时保留已有评论
- 剧情入口点击发出 `CommentPanelEffect.NavigateToDialogue("story-1", "story-1-dialogue")`
- 评论分页成功追加，第二次分页失败时保留已有评论并写入 `"评论加载失败"`

测试文件需要加入这些 helper：

```kotlin
private fun createUseCase(
    initialState: CommentPanelState = CommentPanelState(cardId = "story-1"),
    repository: CommentRepository = FakeCommentRepository(),
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
) = CommentPanelUseCase(
    commentRepository = repository,
    scope = scope,
    stateHolder = initialState.toStateHolder(),
    serviceRegistry = MutableServiceRegistryImpl(),
)
```

同时加入 `sampleUser`、`sampleComment`、`sampleReply`、`EmptyCommentRepository`、`FailingCommentRepository`、`PagedThenFailingCommentRepository` 等测试辅助类。它们的职责是：构造测试用户/评论/回复，模拟空首屏、失败场景、分页先成功后失败。

- [ ] **步骤 2：运行测试确认红灯**

```bash
./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest
```

预期：失败，因为 `CommentPanelUseCase` 还没有 `commentRepository` 参数，也没有处理新事件。

- [ ] **步骤 3：实现 UseCase 的加载类逻辑**

在 `CommentPanelUseCase.kt` 中：
- 构造函数增加 `commentRepository: CommentRepository = FakeCommentRepository()`
- 构造函数增加 `scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)`
- `onEvent` 覆盖所有 `CommentPanelEvent`
- 实现 `loadInitialIfNeeded`
- 实现 `loadInitial`
- 实现 `loadMoreComments`
- 实现 `navigateDialogueIfAvailable`

错误文案固定为：
- 首屏加载失败 toast：`评论加载失败`
- 评论分页失败 state error：`评论加载失败`

未到本任务的函数先保留可编译的最小实现：

```kotlin
private fun expandComment(commentId: String) = Unit
private fun toggleCommentLike(commentId: String) = Unit
private fun expandReplies(commentId: String) = Unit
private fun collapseReplies(commentId: String) = Unit
private fun loadMoreReplies(commentId: String) = Unit
private fun updateInput(text: String) = updateState { it.copy(inputText = text) }
private fun sendComment() = Unit
```

- [ ] **步骤 4：运行测试确认绿灯**

```bash
./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest
```

预期：`BUILD SUCCESSFUL`。

- [ ] **步骤 5：提交任务 3**

```bash
git add biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCase.kt biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt
git commit -m "feat(comment-panel): load comments and dialogue entry"
```

---

## 任务 4：实现长评论展开和评论点赞

**文件：**
- 修改：`CommentPanelUseCaseTest.kt`
- 修改：`CommentPanelUseCase.kt`

- [ ] **步骤 1：添加展开和点赞红灯测试**

追加测试：

```kotlin
@Test
fun `expand comment only changes target comment`() = runTest

@Test
fun `like comment uses optimistic update and server result`() = runTest

@Test
fun `unlike comment never produces negative like count`() = runTest

@Test
fun `like failure rolls back target comment and emits toast`() = runTest
```

断言必须覆盖：
- 展开只影响目标评论
- 点赞成功后 `isLiked == true`、`isLikeSubmitting == false`、`likeCount` 使用服务端结果
- 取消点赞时点赞数不低于 0
- 点赞失败回滚旧评论并发出 `BaseEffect.ShowToast("点赞失败，请重试")`

- [ ] **步骤 2：运行测试确认红灯**

```bash
./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest
```

预期：失败，因为展开和点赞还是 no-op。

- [ ] **步骤 3：实现评论更新 helper 和点赞逻辑**

在 `CommentPanelUseCase.kt` 中新增：

```kotlin
private fun updateComment(commentId: String, transform: (CommentItem) -> CommentItem)
private fun findComment(commentId: String): CommentItem?
```

实现规则：
- `expandComment(commentId)` 只把目标评论 `isExpanded` 置为 `true`
- `toggleCommentLike(commentId)` 先乐观更新
- 乐观更新时设置 `isLikeSubmitting = true`
- 服务端成功后回写 `CommentLikeResult`
- 服务端失败后恢复旧评论并发 toast
- 同一评论 `isLikeSubmitting == true` 时忽略重复点击

- [ ] **步骤 4：运行测试确认绿灯**

```bash
./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest
```

预期：`BUILD SUCCESSFUL`。

- [ ] **步骤 5：提交任务 4**

```bash
git add biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCase.kt biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt
git commit -m "feat(comment-panel): handle comment expansion and likes"
```

---

## 任务 5：实现回复展开、分页、失败和收起

**文件：**
- 修改：`CommentPanelUseCaseTest.kt`
- 修改：`CommentPanelUseCase.kt`

- [ ] **步骤 1：添加回复红灯测试**

追加测试：

```kotlin
@Test
fun `expand replies loads first reply page for target comment`() = runTest

@Test
fun `reply load failure only marks target comment reply section`() = runTest

@Test
fun `load more replies appends and deduplicates target replies`() = runTest

@Test
fun `collapse replies keeps loaded replies`() = runTest
```

断言必须覆盖：
- 展开回复只更新目标评论
- 首次展开会加载回复
- 回复加载失败只写入目标评论 `replySection.errorMessage == "回复加载失败"`
- 加载更多回复追加并按 `replyId` 去重
- 收起回复只改 `isExpanded = false`，保留已加载回复

- [ ] **步骤 2：运行测试确认红灯**

```bash
./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest
```

预期：失败，因为回复相关函数仍是 no-op。

- [ ] **步骤 3：实现回复逻辑**

实现：
- `expandReplies(commentId)`
- `loadReplies(commentId, cursor)`
- `loadMoreReplies(commentId)`
- `collapseReplies(commentId)`

规则：
- 已有回复时再次展开不重新请求
- `isLoading == true` 时忽略重复加载
- 失败只影响目标评论回复区
- 成功后 `ReplySectionState.isExpanded = true`
- 分页状态使用 `PaginationState(nextCursor, hasMore)`

- [ ] **步骤 4：运行测试确认绿灯**

```bash
./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest
```

预期：`BUILD SUCCESSFUL`。

- [ ] **步骤 5：提交任务 5**

```bash
git add biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCase.kt biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt
git commit -m "feat(comment-panel): manage comment replies"
```

---

## 任务 6：实现输入校验和发送评论

**文件：**
- 修改：`CommentPanelUseCaseTest.kt`
- 修改：`CommentPanelUseCase.kt`

- [ ] **步骤 1：添加输入和发送红灯测试**

追加测试：

```kotlin
@Test
fun `input change updates text and clears input error`() = runTest

@Test
fun `blank comment does not send and emits validation toast`() = runTest

@Test
fun `overlong comment does not send and emits validation toast`() = runTest

@Test
fun `send success prepends comment clears input and updates total count`() = runTest

@Test
fun `send failure keeps input and does not add failed comment`() = runTest
```

断言必须覆盖：
- 输入变化更新 `inputText`
- 输入变化清理 `inputErrorMessage` 和 `sendErrorMessage`
- 空评论不发送，错误文案 `请输入评论内容`
- 超长评论不发送，错误文案 `评论不能超过200字`
- 发送成功把新评论插入顶部、清空输入、更新总数
- 发送失败保留输入、不新增评论、错误文案 `发送失败，请重试`

- [ ] **步骤 2：运行测试确认红灯**

```bash
./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest
```

预期：失败，因为发送仍是 no-op，输入更新逻辑也不完整。

- [ ] **步骤 3：实现输入和发送逻辑**

实现规则：
- `OnInputChanged` 更新输入并清理旧错误
- `OnSendClicked` 时如果 `isSendingComment == true`，直接忽略
- trim 后为空：写入 `inputErrorMessage = "请输入评论内容"` 并 toast
- 超过 `CommentPanelMaxInputLength`：写入 `inputErrorMessage = "评论不能超过200字"` 并 toast
- 有效内容：进入 `isSendingComment = true`
- 发送成功：新评论插入顶部、清空输入、退出发送中、更新 `totalCount`
- 发送失败：退出发送中、保留输入、不新增评论、写入 `sendErrorMessage = "发送失败，请重试"` 并 toast

- [ ] **步骤 4：运行测试确认绿灯**

```bash
./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest
```

预期：`BUILD SUCCESSFUL`。

- [ ] **步骤 5：提交任务 6**

```bash
git add biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCase.kt biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt
git commit -m "feat(comment-panel): validate and send comments"
```

---

## 任务 7：Feature 覆盖 Review 和最终验证

**文件：**
- 可能修改：`biz/story/comment-panel/domain/feature.md`
- 检查：`docs/superpowers/specs/2026-05-09-comment-panel-domain-design.md`
- 检查：`CommentPanelUseCaseTest.kt`

- [ ] **步骤 1：逐项 review feature 覆盖**

在终端输出以下映射并逐项确认：

```text
UC-01 -> 初始状态 + OnPanelShown 测试
UC-02 -> 首屏加载成功测试
UC-03 -> 首屏失败保留已有数据测试
UC-04 -> 首屏成功中的 dialogueEntry 状态测试
UC-05 -> NavigateToDialogue effect 测试
UC-06 -> CommentItem 模型契约测试
UC-07 -> 展开长评论测试
UC-08 -> 点赞成功测试
UC-09 -> 取消点赞不低于 0 测试
UC-10 -> 点赞失败回滚测试
UC-11 -> 展开回复成功测试
UC-12 -> 回复加载失败测试
UC-13 -> 加载更多回复测试
UC-14 -> 收起回复测试
UC-15 -> 发送成功测试
UC-16 -> 空评论和超长评论校验测试
UC-17 -> 发送失败测试
UC-18 -> 加载更多评论成功和失败测试
```

- [ ] **步骤 2：运行完整 domain 验证**

```bash
./gradlew :biz:story:comment-panel:domain:test
```

预期：`BUILD SUCCESSFUL`。

- [ ] **步骤 3：运行受影响 presentation 和 app 编译**

```bash
./gradlew :biz:story:comment-panel:presentation:compileDebugKotlin :app:compileDebugKotlin
```

预期：`BUILD SUCCESSFUL`。这一步验证 domain 构造函数变化没有破坏 presentation 的 `CommentPanelViewModel`。

- [ ] **步骤 4：检查 git diff 范围**

```bash
git status --short
git diff --stat
```

预期：只包含 comment-panel domain 文件，以及必要的 presentation 编译适配或 feature 文档同步。

- [ ] **步骤 5：提交最终修正**

如果 步骤 1 或 步骤 3 有修正，提交：

```bash
git add biz/story/comment-panel/domain biz/story/comment-panel/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/presentation/CommentPanelViewModel.kt
git commit -m "test(comment-panel): complete domain coverage"
```

如果 任务 6 后无额外修正，不创建空提交。
