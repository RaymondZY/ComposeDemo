# Story InfoBar Domain 层设计

## 目标

实现 Story InfoBar domain 层的完整功能，覆盖故事标题点击、点赞乐观更新与异常回滚、分享异步获取链接及错误处理、评论和历史占位逻辑。

## 变更范围

仅 `biz/story/infobar/domain/` 层，不触及 presentation。

## State

`InfoBarState` 保持不变，已有字段覆盖全部 usecase。

## Event 变更

| 操作 | 类型 | 说明 |
|------|------|------|
| 删除 | `OnCreatorClicked` | 原作者区域点击，需求已变更 |
| 新增 | `OnStoryTitleClicked` | 故事标题点击跳转详情面板 |

## Effect 变更

| 操作 | 类型 | 说明 |
|------|------|------|
| 删除 | `NavigateToCreatorProfile` | 原作者主页跳转，需求已变更 |
| 新增 | `NavigateToStoryDetail(val cardId: String)` | 故事详情面板跳转 |
| 修改 | `ShowShareSheet` | 改为 `ShowShareSheet(val cardId: String, val shareLink: String)`，携带异步获取的链接 |

其余 effect（`NavigateToComments`、`ShowHistory`）保持不变。

## Repository 新增

```kotlin
interface ShareRepository {
    suspend fun getShareLink(cardId: String): String
}
```

配套 `FakeShareRepository` 用于测试与本地开发。

## UseCase 事件处理流程

### `OnStoryTitleClicked`

直接 dispatch `NavigateToStoryDetail(cardId)`，不修改状态。

### `OnLikeClicked`

1. 记录点击前的旧状态（`oldIsLiked`、`oldLikes`）
2. 乐观更新：立即切换 `isLiked` 并调整 `likes`（`coerceAtLeast(0)`）
3. `likeJob?.cancel()` 取消旧请求
4. launch 新 job 调用 `likeRepository.toggleLike()`
5. `try/catch` 包裹：
   - **成功**：`updateState` 回写服务返回结果
   - **失败**：`updateState` 回滚到旧状态，`dispatchBaseEffect(ShowToast)`

### `OnShareClicked`

1. launch job 调用 `shareRepository.getShareLink(cardId)`
2. `try/catch` 包裹：
   - **成功**：`dispatchEffect(ShowShareSheet(cardId, link))`
   - **失败**：`dispatchBaseEffect(ShowToast("网络失败"))`

### `OnCommentClicked`

直接 dispatch `NavigateToComments(cardId)`，不修改状态。

### `OnHistoryClicked`

直接 dispatch `ShowHistory(cardId)`，不修改状态。

## 错误处理策略

| 场景 | 处理方式 |
|------|---------|
| 点赞请求异常 | 回滚乐观更新 + `BaseEffect.ShowToast` |
| 分享请求异常 | `BaseEffect.ShowToast("网络失败")`，状态不变 |

## 文件变更清单

| 文件 | 变更 |
|------|------|
| `InfoBarEvent.kt` | 删除 `OnCreatorClicked`，新增 `OnStoryTitleClicked` |
| `InfoBarEffect.kt` | 删除 `NavigateToCreatorProfile`，新增 `NavigateToStoryDetail`，修改 `ShowShareSheet` |
| `ShareRepository.kt` | 新增 `ShareRepository` 接口 |
| `FakeShareRepository.kt` | 新增 Fake 实现 |
| `InfoBarUseCase.kt` | 替换事件处理逻辑，新增分享异步调用与错误处理，新增点赞异常回滚 |

## 测试策略

更新 `InfoBarUseCaseTest`，覆盖：

1. `OnStoryTitleClicked` 产生 `NavigateToStoryDetail`，状态不变
2. `OnLikeClicked` 乐观更新切换点赞状态与计数
3. `OnLikeClicked` 异步成功回写服务结果
4. `OnLikeClicked` 异步失败回滚状态并触发 `ShowToast`
5. 快速连续点击点赞只保留最新请求结果
6. `OnShareClicked` 成功触发 `ShowShareSheet`（携带链接）
7. `OnShareClicked` 失败触发 `ShowToast`
