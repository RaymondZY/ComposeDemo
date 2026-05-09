# Comment Panel Domain 层设计

## 目标

实现 `:biz:story:comment-panel:domain` 的完整评论面板业务能力，覆盖 `biz/story/comment-panel/domain/feature.md` 中 UC-01 到 UC-18。范围只包含 domain 层：State、Event、Effect、UseCase、Repository 合约、Fake 数据源与 JUnit 测试，不实现 presentation UI。

## 变更范围

| 模块 | 范围 |
|------|------|
| `biz/story/comment-panel/domain` | 完整实现评论业务状态、事件、效果、用例和 fake repository |
| `biz/story/comment-panel/presentation` | 不做 UI 功能实现；后续只需按 domain 暴露的 state/event 渲染 |

## 核心模型

### `CommentPanelState`

状态保持不可变，覆盖面板级状态与每条评论的局部状态：

| 字段 | 说明 |
|------|------|
| `cardId` | 当前故事上下文 |
| `totalCount` | 当前评论总数 |
| `dialogueEntry` | 顶部对话剧情入口状态 |
| `comments` | 首屏与分页追加后的评论列表 |
| `initialLoadStatus` | 首屏加载状态：`Idle / Loading / Success / Empty / Error` |
| `commentPagination` | 评论分页状态：`nextCursor / hasMore / isLoading / errorMessage` |
| `inputText` | 当前评论输入内容 |
| `isSendingComment` | 是否正在发送评论 |
| `inputErrorMessage` | 输入校验错误 |
| `sendErrorMessage` | 发送失败错误 |

### 评论与回复模型

新增 domain 内部业务模型：

| 类型 | 说明 |
|------|------|
| `CommentUser` | 用户 id、昵称、头像、是否作者 |
| `CommentItem` | 评论 id、用户、正文、发布时间、点赞数、点赞状态、点赞提交中、置顶、长文展开、回复状态 |
| `ReplyItem` | 回复 id、父评论 id、用户、正文、发布时间 |
| `ReplySectionState` | 回复是否展开、是否加载中、回复列表、分页、加载错误 |
| `DialogueEntryState` | 顶部剧情入口是否可用、标题、说明、目标 id |
| `PaginationState` | 通用分页状态 |

`CommentItem` 直接持有 `ReplySectionState`，使回复失败、分页和收起只影响当前评论。

## Event

`CommentPanelEvent` 覆盖所有用户/页面触发：

| Event | 说明 |
|------|------|
| `OnPanelShown` | 首次打开面板，触发首屏加载 |
| `OnRetryInitialLoad` | 重试首屏加载 |
| `OnLoadMoreComments` | 加载更多评论 |
| `OnDialogueEntryClicked` | 进入顶部对话剧情 |
| `OnCommentExpanded(commentId)` | 展开长评论正文 |
| `OnCommentLikeClicked(commentId)` | 点赞或取消点赞评论 |
| `OnRepliesExpanded(commentId)` | 展开某条评论的回复，必要时加载首屏回复 |
| `OnRepliesCollapsed(commentId)` | 收起某条评论的回复 |
| `OnLoadMoreReplies(commentId)` | 加载更多回复 |
| `OnInputChanged(text)` | 更新输入内容 |
| `OnSendClicked` | 发送评论 |

## Effect

`CommentPanelEffect` 只承载 domain 自身需要交给外层执行的一次性结果：

| Effect | 说明 |
|------|------|
| `NavigateToDialogue(cardId: String, targetId: String)` | 顶部对话剧情入口点击成功 |

错误提示使用框架已有 `BaseEffect.ShowToast`。错误状态同时保留在 state 中，供 UI 渲染重试或错误区域。

## Repository 合约

新增 `CommentRepository`，由 domain 定义，配套 `FakeCommentRepository`：

```kotlin
interface CommentRepository {
    suspend fun loadInitial(cardId: String, pageSize: Int): CommentInitialResult
    suspend fun loadMoreComments(cardId: String, cursor: String, pageSize: Int): CommentPage
    suspend fun loadReplies(cardId: String, commentId: String, cursor: String?, pageSize: Int): ReplyPage
    suspend fun setCommentLiked(cardId: String, commentId: String, liked: Boolean): CommentLikeResult
    suspend fun sendComment(cardId: String, content: String): SendCommentResult
}
```

返回模型：

| 类型 | 说明 |
|------|------|
| `CommentInitialResult` | `totalCount`、`dialogueEntry`、首屏 `CommentPage` |
| `CommentPage` | `comments`、`nextCursor`、`hasMore` |
| `ReplyPage` | `replies`、`nextCursor`、`hasMore` |
| `CommentLikeResult` | 评论 id、最终点赞状态、最终点赞数 |
| `SendCommentResult` | 新评论、服务端评论总数 |

Fake 数据源提供固定内容池、分页数据、可配置失败场景，供单元测试稳定覆盖成功和失败路径。

## UseCase 流程

### 初始化与首屏加载

`OnPanelShown` 在 `initialLoadStatus == Idle` 时触发首屏加载。加载中不重复发起请求。成功后更新 `totalCount`、`dialogueEntry`、`comments` 与分页；评论为空时进入 `Empty`。失败时进入 `Error`，但不清空已有可用评论。

`OnRetryInitialLoad` 总是重新加载首屏，用于错误重试。

### 评论分页

`OnLoadMoreComments` 在 `hasMore == true` 且未加载中时触发。成功后按 id 去重追加评论并更新分页。失败只写入 `commentPagination.errorMessage`，保留已有评论。

### 顶部对话剧情入口

`OnDialogueEntryClicked` 仅在入口可用且存在 `targetId` 时 dispatch `NavigateToDialogue(cardId, targetId)`；入口不可用时不修改评论状态。

### 展开长评论

`OnCommentExpanded(commentId)` 将目标评论 `isExpanded` 置为 `true`，重复触发保持幂等，不影响其他评论。

### 评论点赞与取消点赞

`OnCommentLikeClicked(commentId)` 使用乐观更新：

1. 记录目标评论旧状态。
2. 立即切换 `isLiked`，调整 `likeCount`，取消点赞时不低于 0，并将该评论 `isLikeSubmitting` 置为 `true`。
3. 调用 `setCommentLiked` 提交最终目标状态。
4. 成功时用服务端结果回写该评论，并将 `isLikeSubmitting` 置为 `false`。
5. 失败时回滚到旧状态，将 `isLikeSubmitting` 置为 `false`，并发送 `BaseEffect.ShowToast`。

同一评论 `isLikeSubmitting == true` 时忽略重复点击，避免竞态导致计数错乱。

### 回复展开、分页与收起

`OnRepliesExpanded(commentId)` 将回复区置为展开。若该评论还没有加载过回复，则加载首屏回复；若已有回复，只展开不重新请求。回复加载失败只写入该评论的 `replySection.errorMessage`。

`OnLoadMoreReplies(commentId)` 在该评论回复区 `hasMore == true` 且未加载中时触发，成功后追加并去重，失败只影响该评论的回复分页错误。

`OnRepliesCollapsed(commentId)` 只切换 `isExpanded = false`，保留已加载回复，便于再次展开复用。

### 输入校验与发送评论

`OnInputChanged(text)` 更新输入并清理旧的输入错误。`OnSendClicked` 先 trim 内容并校验：

| 规则 | 处理 |
|------|------|
| 空内容 | 不进入发送中，写入 `inputErrorMessage`，发送 `BaseEffect.ShowToast` |
| 超过 200 字 | 不进入发送中，写入 `inputErrorMessage`，发送 `BaseEffect.ShowToast` |
| 正在发送 | 忽略重复发送 |

发送成功后，将新评论插入列表顶部，清空输入，退出发送中，并按服务端结果更新 `totalCount`。发送失败时退出发送中，保留输入内容，不新增失败评论，写入 `sendErrorMessage` 并发送 `BaseEffect.ShowToast`。

## 错误处理策略

| 场景 | 处理 |
|------|------|
| 首屏加载失败 | `initialLoadStatus = Error`，保留已有评论 |
| 评论分页失败 | 写入分页错误，保留已有评论 |
| 点赞失败 | 回滚目标评论，提示失败 |
| 回复加载失败 | 只影响目标评论回复区 |
| 发送校验失败 | 不提交，不改评论列表 |
| 发送失败 | 保留输入，不新增评论，提示失败 |

## 文件变更清单

| 文件 | 变更 |
|------|------|
| `CommentPanelState.kt` | 扩展完整 state 与领域模型 |
| `CommentPanelEvent.kt` | 新增 UC-01 到 UC-18 所需事件 |
| `CommentPanelEffect.kt` | 新增 `NavigateToDialogue` |
| `CommentPanelUseCase.kt` | 实现加载、分页、点赞、回复、发送业务 |
| `CommentRepository.kt` | 新增 repository 合约 |
| `FakeCommentRepository.kt` | 新增可测试 fake 数据源 |
| `CommentPanelUseCaseTest.kt` | 覆盖全部业务路径 |
| `feature.md` | 如实现中发现命名与 feature 不一致，只做同步性修正 |

## 测试策略

更新 `CommentPanelUseCaseTest`，采用纯 JVM 单元测试覆盖：

1. 初始化保留 `cardId`，`OnPanelShown` 触发首屏加载。
2. 首屏加载成功更新评论、总数和剧情入口。
3. 首屏空数据进入空态。
4. 首屏失败进入错误态且不清空已有评论。
5. 顶部剧情入口可用时产生 `NavigateToDialogue`。
6. 长评论展开只影响目标评论。
7. 点赞成功乐观更新并回写服务端结果。
8. 取消点赞成功且点赞数不低于 0。
9. 点赞失败回滚并发出 toast。
10. 展开回复成功只更新目标评论回复区。
11. 回复加载失败只影响目标评论。
12. 回复分页成功追加并去重。
13. 收起回复保留已加载回复。
14. 输入内容更新和错误清理。
15. 空评论与超长评论不会发送。
16. 发送成功插入新评论、清空输入、更新总数。
17. 发送失败保留输入且不新增评论。
18. 评论分页成功追加；分页失败保留已有评论。

所有测试先按 TDD 写红灯，再实现到绿灯。最终验证命令：

```bash
./gradlew :biz:story:comment-panel:domain:test
```
