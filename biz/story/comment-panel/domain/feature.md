# Feature - Comment Panel Domain

> 当前评论面板功能已清空，仅保留后续需求澄清所需的 MVI 骨架。

## 全局功能约束

- **平台无关**：domain 层保持纯 Kotlin，不依赖 Android API。
- **MVI 契约保留**：保留 `CommentPanelState`、`CommentPanelEvent`、`CommentPanelEffect` 和 `CommentPanelUseCase`。
- **无评论业务**：不加载评论、不发送评论、不维护评论列表、不产生评论相关提示。
- **入口参数保留**：状态仅保留当前 `cardId`，用于后续恢复评论面板能力。

## UC-01 初始化空评论面板状态

**前置条件**：评论面板以指定故事打开

**步骤**：

1. 创建 `CommentPanelState`
2. 创建 `CommentPanelUseCase`

**预期结果**：状态仅包含当前故事 `cardId`，不会触发评论加载、发送或副作用。
