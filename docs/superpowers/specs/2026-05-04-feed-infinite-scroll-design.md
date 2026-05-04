# Feed 无限滚动与下拉刷新 — 设计文档

## 背景

当前 `FakeFeedRepository` 仅有 15 条静态 `StoryCard`，分页请求耗尽后返回空列表，导致 Feed 无法无限滚动。同时 UI 层未发送 `OnPreload` 事件，预加载逻辑虽存在于 `FeedUseCase` 但未被触发。本次设计解决这两个问题，并补全顶部下拉刷新交互。

## 目标

1. `FakeFeedRepository` 支持无限请求，从固定数据池中随机组合生成新内容。
2. `FeedScreen` 增加顶部下拉刷新手势。
3. 当当前 Feed 内容还剩 2 个卡片时，自动请求下一页。

## 方案概述

采用**基于现有 MVI 架构的最小改动方案**（方案 1）。复用现有的 `FeedState`、`FeedEvent`、`FeedUseCase` 状态机，仅在以下三个点做针对性改动：

| 改动点 | 内容 |
|--------|------|
| `FakeFeedRepository` | 字段池化 + 随机组合 + 无限生成 |
| `FeedScreen` | 集成 Material3 `PullToRefreshBox` + 监听页面变化发送 `OnPreload` |
| `FeedUseCase` | 逻辑已完备，无需改动 |

## 架构与数据流

```
User Action
    │
    ▼
FeedScreen (Compose UI)
    │  ├─ 下拉手势 → PullToRefreshBox → OnRefresh
    │  └─ 页面滑动 → LaunchedEffect(currentPage) → OnPreload
    ▼
FeedViewModel ──► FeedUseCase
    │
    ▼
FakeFeedRepository
    │
    ▼
FeedState (cards, isLoading, isRefreshing, hasMore, currentPage)
```

## 模块设计

### 1. FakeFeedRepository（service/feed/mock）

**核心思路**：将现有 15 条静态数据的各字段拆解为多个预定义池，每次 `fetchFeed` 时从各池中随机抽样组合成新的 `StoryCard`。

**字段池**：
- `characterNames`: 现有 15 条数据中的角色名去重后的列表
- `characterSubtitles`: 角色副标题列表
- `dialogueTexts`: 对话文本列表
- `storyTitles`: 故事标题列表
- `creatorNames`: 创作者名称列表
- `creatorHandles`: 创作者 Handle 列表
- `backgroundImageUrls`: 背景图片 URL 列表
- `likesRange`, `sharesRange`, `commentsRange`: 数字范围

**生成规则**：
- 每条卡片的 `cardId = "story_${page * pageSize + index}"`，确保全局唯一。
- `cardType = "story"` 固定。
- 各字段从对应池中 `random()` 选取。
- 返回列表长度始终等于 `pageSize`，永不返回 `emptyList()`。

**代码结构**：
```kotlin
class FakeFeedRepository : FeedRepository {
    private val characterNames = listOf("Alice", "Bob", ...)
    private val dialogueTexts = listOf("Hello!", "What's up?", ...)
    // ... 其他字段池

    override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
        val cards = List(pageSize) { index ->
            StoryCard(
                cardId = "story_${page * pageSize + index}",
                characterName = characterNames.random(),
                dialogueText = dialogueTexts.random(),
                // ...
            )
        }
        return Result.success(cards)
    }
}
```

### 2. FeedScreen（biz/feed/presentation）

**下拉刷新**：
- 使用 `androidx.compose.material3.pulltorefresh.PullToRefreshBox`（Material3 官方）包裹 `VerticalPager`。
- `isRefreshing = state.isRefreshing`
- `onRefresh = { viewModel.receiveEvent(FeedEvent.OnRefresh) }`
- `PullToRefreshBox` 已内置嵌套滚动冲突处理：当 `VerticalPager` 在第一个页面且滚动到顶部时，下拉手势被拦截触发刷新；其他情况交给 Pager。

**预加载事件发送**：
```kotlin
val currentPage = pagerState.currentPage
LaunchedEffect(currentPage) {
    if (currentPage >= 0) {
        viewModel.receiveEvent(FeedEvent.OnPreload(currentPage))
    }
}
```

**加载指示器**：
- 顶部刷新指示器：由 `PullToRefreshBox` 自动管理。
- 底部加载更多指示器：保留现有逻辑（`isLoading && !isRefreshing && cards.isNotEmpty()` 时显示 `CircularProgressIndicator`）。
- 首屏全屏加载：保留现有逻辑（`cards.isEmpty() && isLoading` 时显示居中 `CircularProgressIndicator`）。

### 3. FeedUseCase（biz/feed/domain）

**无需改动**。现有逻辑已完整支持：
- `OnRefresh` → `refresh()` → `loadFeed(page=0, isRefresh=true)` → 替换 `cards`
- `OnLoadMore` → `loadMore()` → `loadFeed(page=currentPage, isRefresh=false)` → 追加 `cards`
- `OnPreload(index)` → `preload(index)` → 判断 `index >= cards.size - 2` → 调用 `loadMore()`
- 加载状态互斥：`isLoading || !hasMore` 时直接返回

### 4. FeedState / FeedEvent / FeedEffect

**无需改动**。现有定义已满足需求。

## 状态转换表

| 事件 | 前置状态 | 动作 | 后置状态 |
|------|---------|------|---------|
| `OnRefresh` | 任意 | 调用 `fetchFeed(0, 10)` | `isRefreshing=true` → 成功：`cards=newCards, currentPage=1, hasMore=true` |
| `OnLoadMore` | `!isLoading && hasMore` | 调用 `fetchFeed(currentPage, 10)` | `isLoading=true` → 成功：`cards+=newCards, currentPage++` |
| `OnPreload(i)` | `i >= size-2 && !isLoading && hasMore` | 调用 `loadMore()` | 同 `OnLoadMore` |
| `OnPreload(i)` | `i < size-2 \|\| isLoading \|\| !hasMore` | 无 | 不变 |

## 测试策略（TDD）

开发顺序：
1. 写 `FakeFeedRepositoryTest` → 实现 `FakeFeedRepository`
2. 写 `FeedUseCaseTest`（预加载、状态转换）→ 验证 `FeedUseCase` 无需改动
3. 写 UI 层改动 → 集成验证

### 测试用例

#### FakeFeedRepositoryTest
- `fetchFeed(0, 10) returns 10 non-empty cards`
- `fetchFeed(1, 10) returns 10 cards with unique cardIds`
- `fetchFeed(100, 10) returns 10 cards (never empty)`
- `returned cards have fields from predefined pools`

#### FeedUseCaseTest
- `OnRefresh sets isRefreshing, then updates cards and resets currentPage`
- `OnLoadMore appends cards and increments currentPage`
- `OnPreload when index >= size - 2 triggers loadMore`
- `OnPreload when isLoading is true is ignored`
- `OnPreload when hasMore is false is ignored`
- `consecutive OnLoadMore events are deduplicated`

## 错误处理

- **网络/Repository 失败**：`fetchFeed` 返回 `Result.failure()` 时，`FeedUseCase` 分发 `FeedEffect.ShowError(message)`，加载状态重置。
- **下拉刷新失败**：`isRefreshing` 设为 false，不清理现有 `cards`。
- **加载更多失败**：`isLoading` 设为 false，`hasMore` 保持原值（不置为 false，允许用户重试）。

## 文档产出

- `docs/superpowers/features.md` — 功能清单与验收标准
- `docs/superpowers/usecases.md` — 用例拆解，面向 TDD
- `docs/superpowers/specs/2026-05-04-feed-infinite-scroll-design.md` — 本设计文档

## 后续步骤

1. 用户审查本设计文档和 usecases.md。
2. 调用 `writing-plans` skill 编写实现计划。
3. 按实现计划执行 TDD 开发。
