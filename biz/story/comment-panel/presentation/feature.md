# Feature - Comment Panel Presentation

> 当前评论面板界面功能已清空，仅保留一个空的 MVI Screen 作为后续实现入口。

## 全局功能约束

- **对外入口保留**：保留 `CommentPanelSheet(cardId, onDismissRequest, modifier)`，调用方无需改动。
- **MVI Screen 保留**：`CommentPanelSheet` 内部承载 `CommentPanelScreen`，并通过 `MviScreen<CommentPanelViewModel>` 创建 MVI 作用域。
- **无可见评论 UI**：不展示标题、评论列表、空态、加载态、输入框或发送按钮。
- **可 dismiss**：底部面板仍响应 `onDismissRequest`，不影响故事页面现有状态。

## UC-01 展示空评论面板容器

**前置条件**：用户在故事页面点击评论入口

**步骤**：

1. 页面打开评论面板
2. 面板创建空的 `CommentPanelScreen`

**预期结果**：评论面板仅创建 MVI 作用域和空内容容器，不触发评论业务动作。
