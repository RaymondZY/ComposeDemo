# Comment Panel Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the full comment-panel domain behavior described by UC-01 through UC-18.

**Architecture:** Keep all business rules inside `:biz:story:comment-panel:domain`. The presentation layer remains an empty MVI screen and will later render the domain state and send domain events. Domain state is immutable, repository access is abstracted behind `CommentRepository`, and fake data stays deterministic for JVM tests.

**Tech Stack:** Kotlin, Kotlin Coroutines, custom MVI scaffold, JUnit, kotlinx-coroutines-test

---

## File Map

| File | Responsibility |
|------|----------------|
| `biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelState.kt` | State plus comment, reply, pagination, user, dialogue-entry models |
| `biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelEvent.kt` | UI/domain event contract for UC-01 through UC-18 |
| `biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelEffect.kt` | One-shot domain effects |
| `biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentRepository.kt` | Repository contract and result/page models |
| `biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/FakeCommentRepository.kt` | Deterministic fake implementation for local development and tests |
| `biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCase.kt` | Business rules for loading, pagination, dialogue navigation, likes, replies, input and sending |
| `biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt` | JVM test coverage for all UC-01 through UC-18 paths |
| `biz/story/comment-panel/domain/feature.md` | Only sync if implementation names reveal a mismatch with the already approved feature text |

---

### Task 1: Define Domain Models, Events, Effects, And Repository Contract

**Files:**
- Modify: `biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt`
- Modify: `biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelState.kt`
- Modify: `biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelEvent.kt`
- Modify: `biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelEffect.kt`
- Create: `biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentRepository.kt`

- [ ] **Step 1: Write the failing model contract test**

Replace `CommentPanelUseCaseTest.kt` with this first test class:

```kotlin
package zhaoyun.example.composedemo.story.commentpanel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommentPanelUseCaseTest {
    @Test
    fun `initial state exposes empty loadable comment panel`() {
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
    fun `comment item carries user like expansion and reply state`() {
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

- [ ] **Step 2: Run the model test and verify red**

Run: `./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest`

Expected: FAIL with unresolved references such as `LoadStatus`, `DialogueEntryState`, `CommentItem`, `CommentUser`, and `ReplyItem`.

- [ ] **Step 3: Replace `CommentPanelState.kt`**

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

- [ ] **Step 4: Replace `CommentPanelEvent.kt`**

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

- [ ] **Step 5: Replace `CommentPanelEffect.kt`**

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

- [ ] **Step 6: Create `CommentRepository.kt`**

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

- [ ] **Step 7: Run model test and verify green**

Run: `./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 8: Commit Task 1**

```bash
git add biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelState.kt biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelEvent.kt biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelEffect.kt biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentRepository.kt biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt
git commit -m "feat(comment-panel): define domain contract"
```

---

### Task 2: Add Deterministic Fake Repository

**Files:**
- Create: `biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/FakeCommentRepository.kt`
- Modify: `biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt`

- [ ] **Step 1: Add failing fake repository tests**

Append these tests to `CommentPanelUseCaseTest`:

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

- [ ] **Step 2: Run fake repository tests and verify red**

Run: `./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest`

Expected: FAIL with unresolved reference `FakeCommentRepository`.

- [ ] **Step 3: Create `FakeCommentRepository.kt`**

Implement a deterministic repository with these exact behaviors:

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

- [ ] **Step 4: Run fake repository tests and verify green**

Run: `./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit Task 2**

```bash
git add biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/FakeCommentRepository.kt biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt
git commit -m "feat(comment-panel): add fake comment repository"
```

---

### Task 3: Implement Initial Load, Retry, Dialogue Entry, And Comment Pagination

**Files:**
- Modify: `biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt`
- Modify: `biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCase.kt`

- [ ] **Step 1: Add failing load and pagination tests**

Append tests covering these exact names and assertions:

```kotlin
@Test
fun `panel shown loads first page and dialogue entry`() = runTest {
    val useCase = createUseCase(repository = FakeCommentRepository())

    useCase.receiveEvent(CommentPanelEvent.OnPanelShown)

    assertEquals(LoadStatus.Success, useCase.state.value.initialLoadStatus)
    assertEquals(5, useCase.state.value.totalCount)
    assertEquals(listOf("comment-1", "comment-2", "comment-3", "comment-4", "comment-5"), useCase.state.value.comments.map { it.commentId })
    assertTrue(useCase.state.value.dialogueEntry is DialogueEntryState.Available)
}

@Test
fun `initial load empty page enters empty state`() = runTest {
    val useCase = createUseCase(repository = EmptyCommentRepository())

    useCase.receiveEvent(CommentPanelEvent.OnPanelShown)

    assertEquals(LoadStatus.Empty, useCase.state.value.initialLoadStatus)
    assertEquals(emptyList<CommentItem>(), useCase.state.value.comments)
}

@Test
fun `initial load failure keeps existing comments`() = runTest {
    val existing = sampleComment("existing")
    val useCase = createUseCase(
        initialState = CommentPanelState(cardId = "story-1", comments = listOf(existing), initialLoadStatus = LoadStatus.Success),
        repository = FailingCommentRepository(failInitial = true),
    )

    useCase.receiveEvent(CommentPanelEvent.OnRetryInitialLoad)

    assertEquals(LoadStatus.Error, useCase.state.value.initialLoadStatus)
    assertEquals(listOf(existing), useCase.state.value.comments)
}

@Test
fun `dialogue entry click emits navigation when available`() = runTest {
    val useCase = createUseCase(repository = FakeCommentRepository())
    useCase.receiveEvent(CommentPanelEvent.OnPanelShown)
    val effectDeferred = async { useCase.effect.first() }

    useCase.receiveEvent(CommentPanelEvent.OnDialogueEntryClicked)

    assertEquals(CommentPanelEffect.NavigateToDialogue("story-1", "story-1-dialogue"), effectDeferred.await())
}

@Test
fun `load more comments appends and preserves existing comments on failure`() = runTest {
    val repository = PagedThenFailingCommentRepository()
    val useCase = createUseCase(repository = repository)

    useCase.receiveEvent(CommentPanelEvent.OnPanelShown)
    useCase.receiveEvent(CommentPanelEvent.OnLoadMoreComments)
    val beforeFailure = useCase.state.value.comments
    useCase.receiveEvent(CommentPanelEvent.OnLoadMoreComments)

    assertEquals(listOf("comment-1", "comment-2", "comment-3", "comment-4"), beforeFailure.map { it.commentId })
    assertEquals(beforeFailure, useCase.state.value.comments)
    assertEquals("评论加载失败", useCase.state.value.commentPagination.errorMessage)
}
```

Also add test helpers in the test file:

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

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

private fun sampleUser(id: String = "user-1") = CommentUser(
    userId = id,
    nickname = "测试用户",
    avatarUrl = "https://example.com/$id.png",
)

private fun sampleComment(id: String, replySection: ReplySectionState = ReplySectionState()) = CommentItem(
    commentId = id,
    user = sampleUser(),
    content = "测试评论",
    createdAtText = "刚刚",
    likeCount = 0,
    replySection = replySection,
)

private fun sampleReply(id: String, parentId: String = "comment-1") = ReplyItem(
    replyId = id,
    parentCommentId = parentId,
    user = sampleUser("reply-user"),
    content = "测试回复",
    createdAtText = "刚刚",
)

private class EmptyCommentRepository : CommentRepository by FakeCommentRepository() {
    override suspend fun loadInitial(cardId: String, pageSize: Int): CommentInitialResult {
        return CommentInitialResult(
            totalCount = 0,
            dialogueEntry = DialogueEntryState.Hidden,
            page = CommentPage(emptyList(), nextCursor = null, hasMore = false),
        )
    }
}

private class FailingCommentRepository(
    private val failInitial: Boolean = false,
    private val failLike: Boolean = false,
    private val failReplies: Boolean = false,
    private val failSend: Boolean = false,
) : CommentRepository by FakeCommentRepository() {
    override suspend fun loadInitial(cardId: String, pageSize: Int): CommentInitialResult {
        if (failInitial) error("initial failed")
        return FakeCommentRepository().loadInitial(cardId, pageSize)
    }

    override suspend fun setCommentLiked(cardId: String, commentId: String, liked: Boolean): CommentLikeResult {
        if (failLike) error("like failed")
        return FakeCommentRepository().setCommentLiked(cardId, commentId, liked)
    }

    override suspend fun loadReplies(cardId: String, commentId: String, cursor: String?, pageSize: Int): ReplyPage {
        if (failReplies) error("replies failed")
        return FakeCommentRepository().loadReplies(cardId, commentId, cursor, pageSize)
    }

    override suspend fun sendComment(cardId: String, content: String): SendCommentResult {
        if (failSend) error("send failed")
        return FakeCommentRepository().sendComment(cardId, content)
    }
}

private class PagedThenFailingCommentRepository : CommentRepository by FakeCommentRepository() {
    private var loadMoreCalls = 0

    override suspend fun loadInitial(cardId: String, pageSize: Int): CommentInitialResult {
        val fake = FakeCommentRepository()
        return fake.loadInitial(cardId, pageSize = 2)
    }

    override suspend fun loadMoreComments(cardId: String, cursor: String, pageSize: Int): CommentPage {
        loadMoreCalls += 1
        if (loadMoreCalls > 1) error("load more failed")
        return FakeCommentRepository().loadMoreComments(cardId, cursor, pageSize = 2)
    }
}

private class LikeResultCommentRepository(
    private val result: CommentLikeResult,
) : CommentRepository by FakeCommentRepository() {
    override suspend fun setCommentLiked(cardId: String, commentId: String, liked: Boolean): CommentLikeResult {
        return result
    }
}

private class PagedRepliesRepository : CommentRepository by FakeCommentRepository() {
    override suspend fun loadReplies(cardId: String, commentId: String, cursor: String?, pageSize: Int): ReplyPage {
        val replies = listOf(sampleReply("reply-2", commentId))
        return ReplyPage(replies = replies, nextCursor = null, hasMore = false)
    }
}
```

- [ ] **Step 2: Run tests and verify red**

Run: `./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest`

Expected: FAIL because `CommentPanelUseCase` does not accept `commentRepository` and does not process the new events.

- [ ] **Step 3: Implement load, retry, dialogue, and pagination in `CommentPanelUseCase.kt`**

Replace the constructor and event dispatch shape with:

```kotlin
class CommentPanelUseCase(
    private val commentRepository: CommentRepository = FakeCommentRepository(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    stateHolder: StateHolder<CommentPanelState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<CommentPanelState, CommentPanelEvent, CommentPanelEffect>(stateHolder, serviceRegistry) {
    override suspend fun onEvent(event: CommentPanelEvent) {
        when (event) {
            is CommentPanelEvent.OnPanelShown -> loadInitialIfNeeded()
            is CommentPanelEvent.OnRetryInitialLoad -> loadInitial(force = true)
            is CommentPanelEvent.OnLoadMoreComments -> loadMoreComments()
            is CommentPanelEvent.OnDialogueEntryClicked -> navigateDialogueIfAvailable()
            is CommentPanelEvent.OnCommentExpanded -> expandComment(event.commentId)
            is CommentPanelEvent.OnCommentLikeClicked -> toggleCommentLike(event.commentId)
            is CommentPanelEvent.OnRepliesExpanded -> expandReplies(event.commentId)
            is CommentPanelEvent.OnRepliesCollapsed -> collapseReplies(event.commentId)
            is CommentPanelEvent.OnLoadMoreReplies -> loadMoreReplies(event.commentId)
            is CommentPanelEvent.OnInputChanged -> updateInput(event.text)
            is CommentPanelEvent.OnSendClicked -> sendComment()
        }
    }
}
```

Implement these functions in the same file:

```kotlin
private fun loadInitialIfNeeded() {
    if (currentState.initialLoadStatus == LoadStatus.Idle) loadInitial(force = false)
}

private fun loadInitial(force: Boolean) {
    if (currentState.initialLoadStatus == LoadStatus.Loading) return
    updateState { it.copy(initialLoadStatus = LoadStatus.Loading) }
    scope.launch {
        try {
            val result = commentRepository.loadInitial(currentState.cardId, CommentPanelInitialPageSize)
            updateState {
                it.copy(
                    totalCount = result.totalCount,
                    dialogueEntry = result.dialogueEntry,
                    comments = result.page.comments,
                    initialLoadStatus = if (result.page.comments.isEmpty()) LoadStatus.Empty else LoadStatus.Success,
                    commentPagination = PaginationState(result.page.nextCursor, result.page.hasMore),
                )
            }
        } catch (_: Exception) {
            updateState { it.copy(initialLoadStatus = LoadStatus.Error) }
            dispatchBaseEffect(BaseEffect.ShowToast("评论加载失败"))
        }
    }
}

private fun loadMoreComments() {
    val pagination = currentState.commentPagination
    val cursor = pagination.nextCursor ?: return
    if (!pagination.hasMore || pagination.isLoading) return
    updateState { it.copy(commentPagination = pagination.copy(isLoading = true, errorMessage = null)) }
    scope.launch {
        try {
            val page = commentRepository.loadMoreComments(currentState.cardId, cursor, CommentPanelInitialPageSize)
            updateState {
                it.copy(
                    comments = (it.comments + page.comments).distinctBy { comment -> comment.commentId },
                    commentPagination = PaginationState(page.nextCursor, page.hasMore),
                )
            }
        } catch (_: Exception) {
            updateState {
                it.copy(commentPagination = it.commentPagination.copy(isLoading = false, errorMessage = "评论加载失败"))
            }
        }
    }
}

private fun navigateDialogueIfAvailable() {
    val entry = currentState.dialogueEntry as? DialogueEntryState.Available ?: return
    dispatchEffect(CommentPanelEffect.NavigateToDialogue(currentState.cardId, entry.targetId))
}
```

Leave the later functions as no-op private functions returning `Unit` so the file compiles during this task:

```kotlin
private fun expandComment(commentId: String) = Unit
private fun toggleCommentLike(commentId: String) = Unit
private fun expandReplies(commentId: String) = Unit
private fun collapseReplies(commentId: String) = Unit
private fun loadMoreReplies(commentId: String) = Unit
private fun updateInput(text: String) = updateState { it.copy(inputText = text) }
private fun sendComment() = Unit
```

- [ ] **Step 4: Run tests and verify green**

Run: `./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit Task 3**

```bash
git add biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCase.kt biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt
git commit -m "feat(comment-panel): load comments and dialogue entry"
```

---

### Task 4: Implement Comment Expansion And Like Toggle

**Files:**
- Modify: `biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt`
- Modify: `biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCase.kt`

- [ ] **Step 1: Add failing expansion and like tests**

Append tests for:

```kotlin
@Test
fun `expand comment only changes target comment`() = runTest {
    val useCase = createUseCase()
    useCase.receiveEvent(CommentPanelEvent.OnPanelShown)

    useCase.receiveEvent(CommentPanelEvent.OnCommentExpanded("comment-1"))

    assertTrue(useCase.state.value.comments.first { it.commentId == "comment-1" }.isExpanded)
    assertFalse(useCase.state.value.comments.first { it.commentId == "comment-2" }.isExpanded)
}

@Test
fun `like comment uses optimistic update and server result`() = runTest {
    val useCase = createUseCase()
    useCase.receiveEvent(CommentPanelEvent.OnPanelShown)

    useCase.receiveEvent(CommentPanelEvent.OnCommentLikeClicked("comment-1"))

    val comment = useCase.state.value.comments.first { it.commentId == "comment-1" }
    assertTrue(comment.isLiked)
    assertFalse(comment.isLikeSubmitting)
    assertEquals(13, comment.likeCount)
}

@Test
fun `unlike comment never produces negative like count`() = runTest {
    val liked = sampleComment("liked").copy(isLiked = true, likeCount = 0)
    val useCase = createUseCase(
        initialState = CommentPanelState(cardId = "story-1", comments = listOf(liked)),
        repository = LikeResultCommentRepository(CommentLikeResult("liked", isLiked = false, likeCount = 0)),
    )

    useCase.receiveEvent(CommentPanelEvent.OnCommentLikeClicked("liked"))

    val comment = useCase.state.value.comments.single()
    assertFalse(comment.isLiked)
    assertEquals(0, comment.likeCount)
}

@Test
fun `like failure rolls back target comment and emits toast`() = runTest {
    val initial = sampleComment("comment-1").copy(isLiked = false, likeCount = 4)
    val useCase = createUseCase(
        initialState = CommentPanelState(cardId = "story-1", comments = listOf(initial)),
        repository = FailingCommentRepository(failLike = true),
    )
    val toastDeferred = async { useCase.baseEffect.first() }

    useCase.receiveEvent(CommentPanelEvent.OnCommentLikeClicked("comment-1"))

    assertEquals(initial, useCase.state.value.comments.single())
    assertEquals(BaseEffect.ShowToast("点赞失败，请重试"), toastDeferred.await())
}
```

- [ ] **Step 2: Run tests and verify red**

Run: `./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest`

Expected: FAIL because expansion and like functions are no-op.

- [ ] **Step 3: Implement comment mutation helpers and like logic**

Add helper functions:

```kotlin
private fun updateComment(commentId: String, transform: (CommentItem) -> CommentItem) {
    updateState {
        it.copy(comments = it.comments.map { comment ->
            if (comment.commentId == commentId) transform(comment) else comment
        })
    }
}

private fun findComment(commentId: String): CommentItem? {
    return currentState.comments.firstOrNull { it.commentId == commentId }
}
```

Replace expansion and like functions:

```kotlin
private fun expandComment(commentId: String) {
    updateComment(commentId) { it.copy(isExpanded = true) }
}

private fun toggleCommentLike(commentId: String) {
    val oldComment = findComment(commentId) ?: return
    if (oldComment.isLikeSubmitting) return
    val newLiked = !oldComment.isLiked
    val optimisticLikeCount = if (newLiked) oldComment.likeCount + 1 else (oldComment.likeCount - 1).coerceAtLeast(0)
    updateComment(commentId) {
        it.copy(isLiked = newLiked, likeCount = optimisticLikeCount, isLikeSubmitting = true)
    }
    scope.launch {
        try {
            val result = commentRepository.setCommentLiked(currentState.cardId, commentId, newLiked)
            updateComment(commentId) {
                it.copy(isLiked = result.isLiked, likeCount = result.likeCount.coerceAtLeast(0), isLikeSubmitting = false)
            }
        } catch (_: Exception) {
            updateComment(commentId) { oldComment.copy(isLikeSubmitting = false) }
            dispatchBaseEffect(BaseEffect.ShowToast("点赞失败，请重试"))
        }
    }
}
```

- [ ] **Step 4: Run tests and verify green**

Run: `./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit Task 4**

```bash
git add biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCase.kt biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt
git commit -m "feat(comment-panel): handle comment expansion and likes"
```

---

### Task 5: Implement Replies Expand, Load More, Failure, And Collapse

**Files:**
- Modify: `biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt`
- Modify: `biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCase.kt`

- [ ] **Step 1: Add failing reply tests**

Append these tests:

```kotlin
@Test
fun `expand replies loads first reply page for target comment`() = runTest {
    val useCase = createUseCase(repository = FakeCommentRepository())
    useCase.receiveEvent(CommentPanelEvent.OnPanelShown)

    useCase.receiveEvent(CommentPanelEvent.OnRepliesExpanded("comment-1"))

    val comment = useCase.state.value.comments.first { it.commentId == "comment-1" }
    assertTrue(comment.replySection.isExpanded)
    assertEquals(listOf("reply-1", "reply-2", "reply-3"), comment.replySection.replies.map { it.replyId })
    assertEquals(null, comment.replySection.errorMessage)
}

@Test
fun `reply load failure only marks target comment reply section`() = runTest {
    val first = sampleComment("comment-1")
    val second = sampleComment("comment-2")
    val useCase = createUseCase(
        initialState = CommentPanelState(cardId = "story-1", comments = listOf(first, second)),
        repository = FailingCommentRepository(failReplies = true),
    )

    useCase.receiveEvent(CommentPanelEvent.OnRepliesExpanded("comment-1"))

    val failed = useCase.state.value.comments.first { it.commentId == "comment-1" }
    val untouched = useCase.state.value.comments.first { it.commentId == "comment-2" }
    assertTrue(failed.replySection.isExpanded)
    assertEquals("回复加载失败", failed.replySection.errorMessage)
    assertEquals(null, untouched.replySection.errorMessage)
}

@Test
fun `load more replies appends and deduplicates target replies`() = runTest {
    val initialReply = sampleReply("reply-1")
    val comment = sampleComment(
        "comment-1",
        replySection = ReplySectionState(
            isExpanded = true,
            replies = listOf(initialReply),
            pagination = PaginationState(nextCursor = "cursor-1", hasMore = true),
        ),
    )
    val useCase = createUseCase(
        initialState = CommentPanelState(cardId = "story-1", comments = listOf(comment)),
        repository = PagedRepliesRepository(),
    )

    useCase.receiveEvent(CommentPanelEvent.OnLoadMoreReplies("comment-1"))

    val updated = useCase.state.value.comments.single()
    assertEquals(listOf("reply-1", "reply-2"), updated.replySection.replies.map { it.replyId })
    assertFalse(updated.replySection.pagination.hasMore)
}

@Test
fun `collapse replies keeps loaded replies`() = runTest {
    val comment = sampleComment(
        "comment-1",
        replySection = ReplySectionState(
            isExpanded = true,
            replies = listOf(sampleReply("reply-1")),
            pagination = PaginationState(nextCursor = null, hasMore = false),
        ),
    )
    val useCase = createUseCase(initialState = CommentPanelState(cardId = "story-1", comments = listOf(comment)))

    useCase.receiveEvent(CommentPanelEvent.OnRepliesCollapsed("comment-1"))

    val updated = useCase.state.value.comments.single()
    assertFalse(updated.replySection.isExpanded)
    assertEquals(listOf("reply-1"), updated.replySection.replies.map { it.replyId })
}
```

- [ ] **Step 2: Run tests and verify red**

Run: `./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest`

Expected: FAIL because reply functions are no-op.

- [ ] **Step 3: Implement reply functions**

Replace reply functions:

```kotlin
private fun expandReplies(commentId: String) {
    val comment = findComment(commentId) ?: return
    updateComment(commentId) {
        it.copy(replySection = it.replySection.copy(isExpanded = true, errorMessage = null))
    }
    if (comment.replySection.replies.isEmpty()) {
        loadReplies(commentId, cursor = null)
    }
}

private fun loadMoreReplies(commentId: String) {
    val comment = findComment(commentId) ?: return
    val pagination = comment.replySection.pagination
    val cursor = pagination.nextCursor ?: return
    if (!pagination.hasMore || pagination.isLoading) return
    loadReplies(commentId, cursor)
}

private fun loadReplies(commentId: String, cursor: String?) {
    val comment = findComment(commentId) ?: return
    if (comment.replySection.isLoading) return
    updateComment(commentId) {
        it.copy(replySection = it.replySection.copy(isLoading = true, errorMessage = null))
    }
    scope.launch {
        try {
            val page = commentRepository.loadReplies(currentState.cardId, commentId, cursor, CommentPanelReplyPageSize)
            updateComment(commentId) {
                val existing = if (cursor == null) emptyList() else it.replySection.replies
                it.copy(
                    replySection = it.replySection.copy(
                        isExpanded = true,
                        isLoading = false,
                        replies = (existing + page.replies).distinctBy { reply -> reply.replyId },
                        pagination = PaginationState(page.nextCursor, page.hasMore),
                        errorMessage = null,
                    ),
                )
            }
        } catch (_: Exception) {
            updateComment(commentId) {
                it.copy(replySection = it.replySection.copy(isLoading = false, errorMessage = "回复加载失败"))
            }
        }
    }
}

private fun collapseReplies(commentId: String) {
    updateComment(commentId) {
        it.copy(replySection = it.replySection.copy(isExpanded = false))
    }
}
```

- [ ] **Step 4: Run tests and verify green**

Run: `./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit Task 5**

```bash
git add biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCase.kt biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt
git commit -m "feat(comment-panel): manage comment replies"
```

---

### Task 6: Implement Input Validation And Sending

**Files:**
- Modify: `biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt`
- Modify: `biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCase.kt`

- [ ] **Step 1: Add failing input and send tests**

Append these tests:

```kotlin
@Test
fun `input change updates text and clears input error`() = runTest {
    val useCase = createUseCase(
        initialState = CommentPanelState(cardId = "story-1", inputErrorMessage = "旧错误", sendErrorMessage = "旧发送错误"),
    )

    useCase.receiveEvent(CommentPanelEvent.OnInputChanged("新评论"))

    assertEquals("新评论", useCase.state.value.inputText)
    assertEquals(null, useCase.state.value.inputErrorMessage)
    assertEquals(null, useCase.state.value.sendErrorMessage)
}

@Test
fun `blank comment does not send and emits validation toast`() = runTest {
    val useCase = createUseCase(initialState = CommentPanelState(cardId = "story-1", inputText = "   "))
    val toastDeferred = async { useCase.baseEffect.first() }

    useCase.receiveEvent(CommentPanelEvent.OnSendClicked)

    assertEquals("请输入评论内容", useCase.state.value.inputErrorMessage)
    assertFalse(useCase.state.value.isSendingComment)
    assertEquals(BaseEffect.ShowToast("请输入评论内容"), toastDeferred.await())
}

@Test
fun `overlong comment does not send and emits validation toast`() = runTest {
    val overlong = "a".repeat(CommentPanelMaxInputLength + 1)
    val useCase = createUseCase(initialState = CommentPanelState(cardId = "story-1", inputText = overlong))
    val toastDeferred = async { useCase.baseEffect.first() }

    useCase.receiveEvent(CommentPanelEvent.OnSendClicked)

    assertEquals("评论不能超过200字", useCase.state.value.inputErrorMessage)
    assertEquals(BaseEffect.ShowToast("评论不能超过200字"), toastDeferred.await())
}

@Test
fun `send success prepends comment clears input and updates total count`() = runTest {
    val existing = sampleComment("existing")
    val useCase = createUseCase(
        initialState = CommentPanelState(cardId = "story-1", totalCount = 5, comments = listOf(existing), inputText = "新评论"),
        repository = FakeCommentRepository(),
    )

    useCase.receiveEvent(CommentPanelEvent.OnSendClicked)

    assertEquals("新评论", useCase.state.value.comments.first().content)
    assertEquals("existing", useCase.state.value.comments[1].commentId)
    assertEquals("", useCase.state.value.inputText)
    assertFalse(useCase.state.value.isSendingComment)
    assertEquals(6, useCase.state.value.totalCount)
}

@Test
fun `send failure keeps input and does not add failed comment`() = runTest {
    val existing = sampleComment("existing")
    val useCase = createUseCase(
        initialState = CommentPanelState(cardId = "story-1", comments = listOf(existing), inputText = "失败评论"),
        repository = FailingCommentRepository(failSend = true),
    )
    val toastDeferred = async { useCase.baseEffect.first() }

    useCase.receiveEvent(CommentPanelEvent.OnSendClicked)

    assertEquals("失败评论", useCase.state.value.inputText)
    assertEquals(listOf(existing), useCase.state.value.comments)
    assertFalse(useCase.state.value.isSendingComment)
    assertEquals("发送失败，请重试", useCase.state.value.sendErrorMessage)
    assertEquals(BaseEffect.ShowToast("发送失败，请重试"), toastDeferred.await())
}
```

- [ ] **Step 2: Run tests and verify red**

Run: `./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest`

Expected: FAIL because `sendComment` is no-op and `updateInput` only updates text.

- [ ] **Step 3: Implement input and send logic**

Replace input/send functions:

```kotlin
private fun updateInput(text: String) {
    updateState {
        it.copy(inputText = text, inputErrorMessage = null, sendErrorMessage = null)
    }
}

private fun sendComment() {
    if (currentState.isSendingComment) return
    val content = currentState.inputText.trim()
    val validationError = when {
        content.isBlank() -> "请输入评论内容"
        content.length > CommentPanelMaxInputLength -> "评论不能超过200字"
        else -> null
    }
    if (validationError != null) {
        updateState { it.copy(inputErrorMessage = validationError) }
        dispatchBaseEffect(BaseEffect.ShowToast(validationError))
        return
    }
    updateState { it.copy(isSendingComment = true, sendErrorMessage = null) }
    scope.launch {
        try {
            val result = commentRepository.sendComment(currentState.cardId, content)
            updateState {
                it.copy(
                    comments = (listOf(result.comment) + it.comments).distinctBy { comment -> comment.commentId },
                    totalCount = result.totalCount,
                    inputText = "",
                    isSendingComment = false,
                    inputErrorMessage = null,
                    sendErrorMessage = null,
                    initialLoadStatus = LoadStatus.Success,
                )
            }
        } catch (_: Exception) {
            updateState { it.copy(isSendingComment = false, sendErrorMessage = "发送失败，请重试") }
            dispatchBaseEffect(BaseEffect.ShowToast("发送失败，请重试"))
        }
    }
}
```

- [ ] **Step 4: Run tests and verify green**

Run: `./gradlew :biz:story:comment-panel:domain:test --tests zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelUseCaseTest`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit Task 6**

```bash
git add biz/story/comment-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCase.kt biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt
git commit -m "feat(comment-panel): validate and send comments"
```

---

### Task 7: Feature Coverage Review And Final Verification

**Files:**
- Modify if needed: `biz/story/comment-panel/domain/feature.md`
- Inspect: `docs/superpowers/specs/2026-05-09-comment-panel-domain-design.md`
- Inspect: `biz/story/comment-panel/domain/src/test/kotlin/zhaoyun/example/composedemo/story/commentpanel/domain/CommentPanelUseCaseTest.kt`

- [ ] **Step 1: Review feature coverage**

Create a local checklist in the terminal output mapping:

```text
UC-01 -> initial state + OnPanelShown tests
UC-02 -> first page success test
UC-03 -> first page failure preservation test
UC-04 -> dialogue entry state in first page success test
UC-05 -> NavigateToDialogue effect test
UC-06 -> CommentItem model contract test
UC-07 -> expand comment test
UC-08 -> like success test
UC-09 -> unlike non-negative test
UC-10 -> like failure rollback test
UC-11 -> expand replies success test
UC-12 -> reply load failure test
UC-13 -> load more replies test
UC-14 -> collapse replies test
UC-15 -> send success test
UC-16 -> blank and overlong validation tests
UC-17 -> send failure test
UC-18 -> load more comments success and failure test
```

- [ ] **Step 2: Run full domain verification**

Run: `./gradlew :biz:story:comment-panel:domain:test`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run affected presentation compile check**

Run: `./gradlew :biz:story:comment-panel:presentation:compileDebugKotlin :app:compileDebugKotlin`

Expected: BUILD SUCCESSFUL. This verifies the presentation module still resolves `CommentPanelViewModel` after the domain constructor changes.

- [ ] **Step 4: Inspect git diff**

Run:

```bash
git status --short
git diff --stat
```

Expected: only comment-panel domain files and any intentional feature doc sync are modified.

- [ ] **Step 5: Commit final review fixes**

If Step 1 or Step 3 required corrections, stage only those files:

```bash
git add biz/story/comment-panel/domain biz/story/comment-panel/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/commentpanel/presentation/CommentPanelViewModel.kt
git commit -m "test(comment-panel): complete domain coverage"
```

If no corrections were needed after Task 6, do not create an empty commit.
