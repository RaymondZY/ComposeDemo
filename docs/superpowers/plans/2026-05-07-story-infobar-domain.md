# Story InfoBar Domain Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 Story InfoBar domain 层完整功能，覆盖故事标题点击、点赞异常回滚、分享异步获取链接及错误处理。

**Architecture:** 基于现有 MVI 框架，增量修改 Event/Effect 定义，新增 ShareRepository，在 UseCase 中补充异步调用与错误回滚逻辑。

**Tech Stack:** Kotlin, Kotlin Coroutines, JUnit, kotlinx-coroutines-test

---

## File Map

| 文件 | 责任 |
|------|------|
| `InfoBarEvent.kt` | 删除 `OnCreatorClicked`，新增 `OnStoryTitleClicked` |
| `InfoBarEffect.kt` | 删除 `NavigateToCreatorProfile`，新增 `NavigateToStoryDetail`，修改 `ShowShareSheet` 携带链接 |
| `ShareRepository.kt` | 定义分享链接获取接口 |
| `FakeShareRepository.kt` | 本地 Fake 实现 |
| `InfoBarUseCase.kt` | 事件处理逻辑：标题点击、点赞异常回滚、分享异步+错误处理 |
| `InfoBarUseCaseTest.kt` | 覆盖全部 usecase 的单元测试 |

---

### Task 1: 更新 Event 与 Effect 定义

**Files:**
- Modify: `biz/story/infobar/domain/src/main/kotlin/zhaoyun/example/composedemo/story/infobar/domain/InfoBarEvent.kt`
- Modify: `biz/story/infobar/domain/src/main/kotlin/zhaoyun/example/composedemo/story/infobar/domain/InfoBarEffect.kt`

- [ ] **Step 1: 修改 InfoBarEvent.kt**

替换整个文件内容：

```kotlin
package zhaoyun.example.composedemo.story.infobar.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class InfoBarEvent : UiEvent {
    data object OnLikeClicked : InfoBarEvent()
    data object OnShareClicked : InfoBarEvent()
    data object OnCommentClicked : InfoBarEvent()
    data object OnHistoryClicked : InfoBarEvent()
    data object OnStoryTitleClicked : InfoBarEvent()
}
```

- [ ] **Step 2: 修改 InfoBarEffect.kt**

替换整个文件内容：

```kotlin
package zhaoyun.example.composedemo.story.infobar.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect

sealed class InfoBarEffect : UiEffect {
    data class ShowShareSheet(val cardId: String, val shareLink: String) : InfoBarEffect()
    data class NavigateToComments(val cardId: String) : InfoBarEffect()
    data class ShowHistory(val cardId: String) : InfoBarEffect()
    data class NavigateToStoryDetail(val cardId: String) : InfoBarEffect()
}
```

- [ ] **Step 3: 编译检查**

Run: `./gradlew :biz:story:infobar:domain:compileKotlin`
Expected: BUILD SUCCESSFUL（此时 UseCase 引用旧类型会编译失败，属于预期，下一步修复）

---

### Task 2: 新增 ShareRepository

**Files:**
- Create: `biz/story/infobar/domain/src/main/kotlin/zhaoyun/example/composedemo/story/infobar/domain/ShareRepository.kt`
- Create: `biz/story/infobar/domain/src/main/kotlin/zhaoyun/example/composedemo/story/infobar/domain/FakeShareRepository.kt`

- [ ] **Step 1: 创建 ShareRepository.kt**

```kotlin
package zhaoyun.example.composedemo.story.infobar.domain

interface ShareRepository {
    suspend fun getShareLink(cardId: String): String
}
```

- [ ] **Step 2: 创建 FakeShareRepository.kt**

```kotlin
package zhaoyun.example.composedemo.story.infobar.domain

class FakeShareRepository : ShareRepository {
    override suspend fun getShareLink(cardId: String): String {
        return "https://example.com/share/$cardId"
    }
}
```

- [ ] **Step 3: 编译检查**

Run: `./gradlew :biz:story:infobar:domain:compileKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: 更新 InfoBarUseCase

**Files:**
- Modify: `biz/story/infobar/domain/src/main/kotlin/zhaoyun/example/composedemo/story/infobar/domain/InfoBarUseCase.kt`

- [ ] **Step 1: 修改 InfoBarUseCase.kt**

替换整个文件内容：

```kotlin
package zhaoyun.example.composedemo.story.infobar.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class InfoBarUseCase(
    private val cardId: String,
    private val likeRepository: LikeRepository = FakeLikeRepository(),
    private val shareRepository: ShareRepository = FakeShareRepository(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    stateHolder: StateHolder<InfoBarState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<InfoBarState, InfoBarEvent, InfoBarEffect>(
    stateHolder,
    serviceRegistry,
) {
    private var likeJob: Job? = null

    override suspend fun onEvent(event: InfoBarEvent) {
        when (event) {
            is InfoBarEvent.OnLikeClicked -> {
                val oldState = currentState
                val newIsLiked = !oldState.isLiked
                val newLikes = if (newIsLiked) oldState.likes + 1 else oldState.likes - 1
                updateState { it.copy(isLiked = newIsLiked, likes = newLikes.coerceAtLeast(0)) }

                likeJob?.cancel()
                likeJob = scope.launch {
                    try {
                        val result = likeRepository.toggleLike(cardId, newIsLiked, oldState.likes)
                        updateState { it.copy(isLiked = result.isLiked, likes = result.likes.coerceAtLeast(0)) }
                    } catch (_: Exception) {
                        updateState { oldState }
                        dispatchBaseEffect(BaseEffect.ShowToast("操作失败，请重试"))
                    }
                }
            }

            is InfoBarEvent.OnShareClicked -> {
                scope.launch {
                    try {
                        val link = shareRepository.getShareLink(cardId)
                        dispatchEffect(InfoBarEffect.ShowShareSheet(cardId, link))
                    } catch (_: Exception) {
                        dispatchBaseEffect(BaseEffect.ShowToast("网络失败"))
                    }
                }
            }

            is InfoBarEvent.OnCommentClicked -> {
                dispatchEffect(InfoBarEffect.NavigateToComments(cardId))
            }

            is InfoBarEvent.OnHistoryClicked -> {
                dispatchEffect(InfoBarEffect.ShowHistory(cardId))
            }

            is InfoBarEvent.OnStoryTitleClicked -> {
                dispatchEffect(InfoBarEffect.NavigateToStoryDetail(cardId))
            }
        }
    }
}
```

- [ ] **Step 2: 编译检查**

Run: `./gradlew :biz:story:infobar:domain:compileKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 4: 更新 InfoBarUseCaseTest

**Files:**
- Modify: `biz/story/infobar/domain/src/test/kotlin/zhaoyun/example/composedemo/story/infobar/domain/InfoBarUseCaseTest.kt`

- [ ] **Step 1: 修改 createUseCase 辅助函数**

在 `createUseCase` 中增加 `shareRepository` 参数：

```kotlin
private fun createUseCase(
    cardId: String = "test-1",
    initialState: InfoBarState = InfoBarState(),
    likeRepository: LikeRepository = FakeLikeRepository { _, isLiked, currentLikes ->
        LikeResult(isLiked = isLiked, likes = if (isLiked) currentLikes + 1 else (currentLikes - 1).coerceAtLeast(0))
    },
    shareRepository: ShareRepository = FakeShareRepository(),
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
) = InfoBarUseCase(
    cardId = cardId,
    likeRepository = likeRepository,
    shareRepository = shareRepository,
    scope = scope,
    stateHolder = initialState.toStateHolder(),
    serviceRegistry = MutableServiceRegistryImpl(),
)
```

- [ ] **Step 2: 删除 `OnCreatorClicked` 测试，替换为 `OnStoryTitleClicked` 测试**

删除原有的 `点击作者区域发送NavigateToCreatorProfile效果` 测试。

新增测试：

```kotlin
@Test
fun `点击故事标题发送NavigateToStoryDetail效果`() = runTest {
    val useCase = createUseCase(cardId = "story-1")
    val effectDeferred = async { useCase.effect.first() }
    useCase.receiveEvent(InfoBarEvent.OnStoryTitleClicked)
    assertEquals(
        InfoBarEffect.NavigateToStoryDetail("story-1"),
        effectDeferred.await(),
    )
}
```

- [ ] **Step 3: 更新分享测试（成功路径）**

将原有 `点击分享发送ShowShareSheet效果且不改变状态` 测试替换为：

```kotlin
@Test
fun `点击分享成功后发送ShowShareSheet效果且不改变状态`() = runTest {
    val initialState = InfoBarState(likes = 3, shares = 2, comments = 1, isLiked = true)
    val useCase = createUseCase(cardId = "story-1", initialState = initialState)

    val effectDeferred = async { useCase.effect.first() }
    useCase.receiveEvent(InfoBarEvent.OnShareClicked)

    assertEquals(
        InfoBarEffect.ShowShareSheet("story-1", "https://example.com/share/story-1"),
        effectDeferred.await(),
    )
    assertEquals(initialState, useCase.state.value)
}
```

- [ ] **Step 4: 新增分享失败测试**

在分享成功测试之后新增：

```kotlin
@Test
fun `点击分享失败后发送ShowToast效果且不改变状态`() = runTest {
    val initialState = InfoBarState(likes = 3, shares = 2, comments = 1, isLiked = true)
    val failingShareRepository = object : ShareRepository {
        override suspend fun getShareLink(cardId: String): String {
            throw RuntimeException("network error")
        }
    }
    val useCase = createUseCase(
        cardId = "story-1",
        initialState = initialState,
        shareRepository = failingShareRepository,
    )

    val baseEffectDeferred = async { useCase.baseEffect.first() }
    useCase.receiveEvent(InfoBarEvent.OnShareClicked)

    assertEquals(
        BaseEffect.ShowToast("网络失败"),
        baseEffectDeferred.await(),
    )
    assertEquals(initialState, useCase.state.value)
}
```

- [ ] **Step 5: 新增点赞失败回滚测试**

在快速点击测试之前新增：

```kotlin
@Test
fun `点赞请求失败后回滚乐观更新并发送ShowToast`() = runTest {
    val failingRepository = object : LikeRepository {
        override suspend fun toggleLike(cardId: String, isLiked: Boolean, currentLikes: Int): LikeResult {
            throw RuntimeException("server error")
        }
    }
    val useCase = createUseCase(
        initialState = InfoBarState(isLiked = false, likes = 5),
        likeRepository = failingRepository,
    )

    val baseEffectDeferred = async { useCase.baseEffect.first() }
    useCase.receiveEvent(InfoBarEvent.OnLikeClicked)

    // 乐观更新后立即断言
    assertTrue(useCase.state.value.isLiked)
    assertEquals(6, useCase.state.value.likes)

    // 等待异常处理完成
    assertEquals(
        BaseEffect.ShowToast("操作失败，请重试"),
        baseEffectDeferred.await(),
    )

    // 回滚到原始状态
    assertFalse(useCase.state.value.isLiked)
    assertEquals(5, useCase.state.value.likes)
}
```

- [ ] **Step 6: 运行全部测试**

Run: `./gradlew :biz:story:infobar:domain:test`
Expected: BUILD SUCCESSFUL，所有测试通过

---

## Self-Review Checklist

- [ ] Spec coverage: UC-01~UC-10 全部有对应实现或已有覆盖
- [ ] Placeholder scan: 无 TBD、TODO、无模糊描述
- [ ] Type consistency: `ShowShareSheet(cardId, shareLink)`、`NavigateToStoryDetail(cardId)`、`OnStoryTitleClicked` 在全 plan 中一致
- [ ] `BaseEffect` 导入路径正确: `zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect`
