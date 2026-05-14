# Comment Panel Platform 层设计

## 目标

实现 `:biz:story:comment-panel:platform` 的完整评论面板展示与交互能力，并补全 `biz/story/comment-panel/platform/feature.md` 作为平台层功能契约。范围覆盖底部面板容器、加载/空/错误/成功状态、评论列表、顶部对话入口、点赞、回复、分页、输入与发送，以及一次性提示和导航效果承接。

本次设计保持 `:biz:story:comment-panel:core` API 稳定，不修改 core 的 State、Event、Effect、UseCase 和 Repository 合约。Platform 层只做业务状态到 Compose UI 的简单映射，所有用户行为通过既有 `CommentPanelEvent` 转发给 ViewModel。

## 变更范围

| 模块 | 范围 |
|------|------|
| `biz/story/comment-panel/platform` | 完整实现评论面板 UI、ViewModel 连接、Effect 收集、Compose 测试和 DI 测试 |
| `biz/story/comment-panel/platform/feature.md` | 从空占位契约更新为完整平台功能契约 |
| `biz/story/platform` | 为 `CommentPanelSheet` 接入对话入口回调，当前可先关闭面板或保留空处理，不改变故事页主体结构 |
| `biz/story/comment-panel/core` | 不改 API；本轮实现必须基于现有 core 状态、事件和效果完成 |

## Platform Feature 契约

`platform/feature.md` 需要先改写为完整 UI 契约，使用 UC 描述平台可见行为，不包含类名、方法名、字段名、测试函数名或 DI 细节。必须覆盖以下 UC：

1. 打开和关闭评论底部面板，保持当前故事上下文。
2. 面板首次展示后触发评论加载，重复展示不造成重复请求或状态错乱。
3. 首屏加载中展示 loading。
4. 首屏空数据展示空态，不展示评论列表。
5. 首屏失败展示可重试错误态，用户可重试加载。
6. 首屏成功展示评论总数、顶部区域和评论列表。
7. 顶部对话剧情入口可用时展示入口，不可用或隐藏时不占用主要内容。
8. 点击顶部对话入口时请求外层进入对应剧情。
9. 评论项展示用户、作者标识、置顶标识、正文、时间、点赞数、回复摘要等基础信息。
10. 长评论可展开，重复展开不影响其它评论。
11. 点赞或取消点赞时展示提交中状态，并避免用户误以为可重复提交。
12. 点赞失败时由平台承接业务提示，评论项回到业务层给出的最终状态。
13. 回复可展开、收起；已加载回复再次展开时复用当前内容。
14. 回复加载中、失败、加载更多和无更多状态只影响所属评论。
15. 评论列表分页加载中、失败重试和无更多状态显示在列表底部。
16. 输入栏展示当前输入、校验错误、发送错误和发送中状态。
17. 有效输入可发送；发送成功后输入清空并展示新增评论。
18. 发送失败时保留输入内容并展示失败反馈。
19. Toast 等一次性提示由平台承接，不阻塞底部面板继续交互。
20. 面板内容在小屏幕和键盘弹出场景下保持可滚动、输入栏可见。

## 架构设计

### `CommentPanelSheet`

`CommentPanelSheet` 继续作为底部容器入口：

- 接收 `cardId`、`onDismissRequest`，新增可选 `onDialogueRequested(cardId, targetId)` 回调。
- 用 `screenViewModel` 创建 `CommentPanelViewModel`，初始状态为 `CommentPanelState(cardId = cardId)`。
- `LaunchedEffect(viewModel)` 发送一次 `CommentPanelEvent.OnPanelShown`。
- 收集 `CommentPanelEffect.NavigateToDialogue`，转发给 `onDialogueRequested`。
- 收集 `BaseEffect.ShowToast`，使用 Android Toast 展示；当前 CommentPanel core 只会产生 toast 类 base effect，本轮不新增其它 base effect 处理。
- 渲染 `ModalBottomSheet`，内部调用纯 UI 组件 `CommentPanelContent`。

这与 `SharePanelSheet` 的职责边界一致：容器负责平台能力和一次性效果，内容组件负责展示与事件回调。

### `CommentPanelContent`

`CommentPanelContent` 是主要纯 Compose 渲染入口，参数包含：

- `state: CommentPanelState`
- `onRetryInitialLoad`
- `onDialogueClick`
- `onExpandComment`
- `onToggleLike`
- `onExpandReplies`
- `onCollapseReplies`
- `onLoadMoreReplies`
- `onLoadMoreComments`
- `onInputChange`
- `onSendClick`

内容拆分为小组件，避免单个文件过大：

| 组件 | 职责 |
|------|------|
| `CommentPanelHeader` | 展示标题、评论总数和关闭区域下方的整体结构信息 |
| `DialogueEntryRow` | 根据入口状态展示可点击入口或隐藏 |
| `CommentLoadStateContent` | 处理首屏 loading、empty、error、success 分支 |
| `CommentList` | 使用 `LazyColumn` 展示评论和列表底部分页状态 |
| `CommentRow` | 展示单条评论基础信息、长文展开、点赞、回复入口 |
| `ReplySection` | 展示回复列表、回复 loading、错误、加载更多和收起 |
| `CommentInputBar` | 展示输入框、错误文案、发送按钮和发送中状态 |

## UI 状态映射

### 首屏状态

| Core 状态 | Platform 展示 |
|-----------|---------------|
| `Idle` | 初始空容器，可显示轻量 loading 占位，等待 `OnPanelShown` 触发 |
| `Loading` | 面板主体显示进度提示，输入栏可保留但发送按钮不可用 |
| `Empty` | 展示空评论提示和输入栏 |
| `Error` | 展示错误提示和重试按钮，点击发送 `OnRetryInitialLoad` |
| `Success` | 展示顶部入口、评论列表、分页状态和输入栏 |

### 评论项

评论项展示头像占位或头像图片、昵称、作者标识、置顶标识、发布时间、正文、点赞数、回复数。`canExpand && !isExpanded` 时展示展开入口；点击后发送 `OnCommentExpanded(commentId)`。点赞按钮在 `isLikeSubmitting` 时展示禁用或进度状态，点击发送 `OnCommentLikeClicked(commentId)`。

### 回复区

未展开时，如果 `replyCount > 0`，展示“查看回复”入口。展开时展示已加载回复；`isLoading` 时展示当前评论下的局部 loading；`errorMessage != null` 时展示局部错误和重试；`pagination.hasMore` 时展示加载更多回复；收起入口发送 `OnRepliesCollapsed(commentId)`。

### 列表分页

评论列表底部根据 `commentPagination` 展示：

- `isLoading == true`：加载更多进度。
- `errorMessage != null`：错误文案和重试按钮，点击发送 `OnLoadMoreComments`。
- `hasMore == true` 且非加载中：加载更多按钮。
- `hasMore == false` 且评论非空：可显示轻量“没有更多评论”。

### 输入栏

输入栏固定在面板底部内容区域，随键盘由 bottom sheet/Compose insets 处理。输入变化发送 `OnInputChanged(text)`。发送按钮在 `isSendingComment` 时禁用并显示发送中状态；输入为空时按钮可禁用，最终校验仍由 core 决定。`inputErrorMessage` 和 `sendErrorMessage` 以输入栏附近短文案展示，同时 Toast 由 base effect 承接。

## 数据流

```text
CommentPanelSheet
  -> screenViewModel + initial CommentPanelState(cardId)
  -> OnPanelShown
  -> CommentPanelViewModel
  -> CommentPanelUseCase
  -> CommentPanelState
  -> CommentPanelContent
  -> 用户操作
  -> CommentPanelEvent
```

所有业务规则仍在 core：加载、分页、点赞乐观更新、回复状态、输入校验和发送结果。Platform 层不重新推导复杂业务状态，只根据 state 做显示/隐藏、启用/禁用、颜色和文案选择。

## 错误处理

| 场景 | Platform 处理 |
|------|---------------|
| 首屏加载失败 | 展示错误态和重试按钮 |
| 评论分页失败 | 在列表底部展示错误和重试 |
| 回复加载失败 | 在对应评论回复区展示错误和重试 |
| 点赞失败 | 收集 toast，评论项按 core 回滚后的状态展示 |
| 输入校验失败 | 输入栏展示错误文案并收集 toast |
| 发送失败 | 保留输入，展示发送错误并收集 toast |
| 对话入口不可用 | 不展示可点击入口，不发送导航请求 |

## 测试策略

### JVM 测试

保留并扩展 `CommentPanelPlatformModuleTest`，验证 `CommentPanelViewModel` 可在 `MviKoinScopes.Screen` 内解析，初始 `cardId` 正确。

### Compose androidTest

优先对 `CommentPanelContent` 做 content-level 测试，直接传入构造好的 `CommentPanelState` 和记录回调，覆盖纯 UI 状态与交互：

1. loading、empty、error、success 分支可见。
2. 错误重试点击触发重试回调。
3. 顶部对话入口展示和点击触发回调。
4. 评论基础信息、作者标识、置顶标识、点赞数和回复数可见。
5. 长评论展开入口点击触发回调。
6. 点赞按钮点击触发回调，提交中状态禁用或显示进度。
7. 回复展开、收起、加载更多和回复错误重试触发对应回调。
8. 评论分页加载、失败重试、加载更多和无更多状态可见。
9. 输入变化和发送点击触发对应回调。
10. 发送中、输入错误、发送错误状态可见。

另保留一个轻量 `CommentPanelSheet` 集成测试，验证真实 ViewModel 打开后能够从 fake repository 加载并展示评论内容。该测试只覆盖主路径，避免把 core 异步细节重复测一遍。

## 验证命令

```bash
./gradlew :biz:story:comment-panel:platform:testDebugUnitTest
./gradlew :biz:story:comment-panel:platform:connectedDebugAndroidTest
./gradlew :biz:story:platform:compileDebugKotlin
```

如本地无可用设备，至少运行 JVM 测试和 Debug Kotlin 编译，并明确记录 androidTest 未运行。
