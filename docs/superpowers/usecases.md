# Feed 无限滚动与下拉刷新 — 用例文档

## UC-1: 用户下拉刷新 Feed

**前置条件**: Feed 页面已加载，显示至少一个卡片。

**主流程**:
1. 用户在第一个卡片顶部执行下拉手势。
2. UI 显示刷新指示器，发送 `FeedEvent.OnRefresh`。
3. `FeedUseCase` 调用 `feedRepository.fetchFeed(page=0, pageSize=10)`。
4. Repository 返回新的一组卡片。
5. `FeedState` 更新：`cards` 替换为新列表，`isRefreshing=false`，`currentPage=1`，`hasMore=true`。

**异常流程**:
- 4a. 请求失败：`isRefreshing=false`，分发 `FeedEffect.ShowError(message)`。

## UC-2: 用户滑动触发加载更多

**前置条件**: Feed 已加载多页内容，当前不是最后一页。

**主流程**:
1. 用户滑动到卡片列表末尾附近（剩余 2 个）。
2. UI 发送 `FeedEvent.OnPreload(index)`。
3. `FeedUseCase` 判断满足预加载条件，调用 `loadMore()`。
4. `feedRepository.fetchFeed(page=currentPage, pageSize=10)` 被调用。
5. Repository 返回下一页卡片。
6. `FeedState` 更新：新卡片追加到 `cards` 尾部，`isLoading=false`，`currentPage` 递增。

**异常流程**:
- 3a. 已经在加载中：忽略本次预加载事件。
- 3b. `hasMore == false`：忽略本次预加载事件。
- 5a. 请求失败：`isLoading=false`，分发 `FeedEffect.ShowError(message)`，`hasMore` 保持原值。

## UC-3: FakeFeedRepository 无限生成内容

**前置条件**: Repository 已初始化。

**主流程**:
1. 调用方传入任意 `page >= 0` 和 `pageSize > 0`。
2. Repository 从预定义字段池中随机抽样，组合生成 `pageSize` 条 `StoryCard`。
3. 每条卡片的 `cardId` 基于 `page` 和 `index` 生成，确保全局唯一。
4. 返回 `Result.success(cards)`，列表长度等于 `pageSize`。

**字段池示例**:
- `characterNames`: ["Alice", "Bob", "Carol", ...]
- `dialogueTexts`: ["Hello!", "What's up?", ...]
- `backgroundImageUrls`: ["https://example.com/img1.jpg", ...]
- `creatorNames`: ["Creator A", "Creator B", ...]

## UC-4: 加载状态互斥与边界保护

**前置条件**: Feed 正在加载中。

**主流程**:
1. 当 `isLoading == true` 时，重复收到 `OnLoadMore` 或 `OnPreload`。
2. `FeedUseCase` 直接返回，不触发新的网络请求。
3. 当 `isRefreshing == true` 时，忽略 `OnLoadMore` 和 `OnPreload`。
