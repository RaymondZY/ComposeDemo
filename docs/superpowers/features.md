# Feed 无限滚动与下拉刷新 — 功能清单

## Feature 1: FakeFeedRepository 无限内容生成
- **描述**: `FakeFeedRepository` 从固定字段池中随机组合生成无限不同的 `StoryCard`，不再耗尽后返回空列表。
- **验收标准**:
  1. `fetchFeed(page, pageSize)` 对任意 `page >= 0` 都返回 `pageSize` 条非空卡片。
  2. 不同 page 返回的卡片 `cardId` 不重复。
  3. 卡片字段（角色名、对话、图片 URL 等）来自预定义的数据池。
  4. 每次请求的内容具有一定的随机多样性。

## Feature 2: Feed 顶部下拉刷新
- **描述**: 用户在 Feed 第一个卡片顶部下拉时，触发内容刷新，替换当前所有卡片。
- **验收标准**:
  1. 在第一个卡片顶部下拉时显示刷新指示器。
  2. 下拉刷新触发 `FeedEvent.OnRefresh`。
  3. 刷新完成后，`cards` 被新数据替换，`currentPage` 重置。
  4. 非第一个卡片或不在顶部时，下拉手势交给 VerticalPager 正常翻页。

## Feature 3: Feed 加载更多指示器
- **描述**: 当正在加载更多内容时，在 Feed 底部显示加载指示器。
- **验收标准**:
  1. `isLoading == true && !isRefreshing && cards.isNotEmpty()` 时底部显示 `CircularProgressIndicator`。
  2. 加载完成后指示器消失。

## Feature 4: 剩余 2 个卡片时预加载下一页
- **描述**: 当用户滑动到距离当前卡片列表末尾还剩 2 个卡片时，自动触发下一页数据加载。
- **验收标准**:
  1. 当前页码 `index >= cards.size - 2` 时触发 `FeedEvent.OnPreload(index)`。
  2. 预加载触发后 `isLoading` 变为 true，底部显示加载指示器。
  3. 如果已经在加载中（`isLoading == true`），不重复触发。
  4. 如果没有更多数据（`hasMore == false`），不触发预加载。
