# Feed Presentation 层设计

## 目标

基于已稳定的 domain 层 API，实现 Feed 页面的刷新、加载更多、错误提示等 UI 交互。

## 交互设计

### 1. 下拉刷新

- 在首个卡片顶部叠加自定义下滑手势检测
- 向下滑动超过阈值时触发 `FeedEvent.OnRefresh`
- 刷新中顶部显示 `LinearProgressIndicator`
- 刷新完成后指示器消失

### 2. 静默加载更多

- `VerticalPager` 的 `page` lambda 中检测 `page >= cards.size - 3`
- 条件满足时发送 `FeedEvent.OnPreload(page)`
- domain 层自动转换为 `OnLoadMore`

### 3. Footer

- 底部叠加悬浮 Box
- `isLoading && !isRefreshing`：显示 `CircularProgressIndicator`
- `!hasMore`：显示"没有更多内容"文字
- 其他情况：不显示

### 4. Effect 处理

- `ShowRefreshError`：弹出 Toast "刷新失败，请重试"
- `ShowLoadMoreError`：弹出 Toast "加载失败，请重试"
- 在 `MviScreen` 的 `onBaseEffect` 中处理 `BaseEffect.ShowToast`

## 组件变更

| 文件 | 变更 |
|------|------|
| `FeedScreen.kt` | 增加下滑手势检测、Footer 叠加层、Effect 收集、preload 触发 |
| `FeedViewModel.kt` | 无需变更，domain API 已稳定 |

## 测试策略

- `FeedScreenTest`（androidTest）：验证下拉刷新触发、Footer 显示状态
