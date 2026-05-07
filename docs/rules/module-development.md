# 模块开发规范

## 1. 需求描述

每个模块的实现需求必须在 **对应子层的 `usecases.md`** 中进行描述（如 `domain/usecases.md`、`presentation/usecases.md`）。该文件是模块的功能契约，开发者和测试者都以此为依据。

```
biz/feed/
├── domain/
│   ├── usecases.md      ← 模块需求描述
│   ├── src/main/...
│   └── src/test/...     ← 功能与测试覆盖
└── presentation/
    ├── usecases.md      ← 可选，presentation 层特定需求
    ├── src/main/...
    └── src/androidTest/...
```

## 2. 测试覆盖要求

功能代码和测试代码都**必须覆盖 `usecases.md` 中描述的所有需求**。不允许出现需求已描述但无对应实现或测试的情况。

`usecases.md` 中的测试描述**必须与项目代码无关**，仅作为需求功能点的描述。不得出现具体的类名、方法名、字段名、Toast 文案等代码相关细节。

## 3. 开发顺序：从内向外

遵循**从内向外**的开发顺序，确保内层稳定后再构建外层。

```
Step 1: domain 层
  ├─ 编写 UseCase 业务逻辑
  ├─ 编写 JUnit 单元测试（纯 Kotlin，JVM 快速运行）
  └─ 验证所有 usecases.md 需求已覆盖

Step 2: platform（presentation）层
  ├─ 基于稳定的 domain API 开发 ViewModel / Composable
  ├─ 编写 JUnit + androidTest 测试
  └─ 验证所有 usecases.md 需求已覆盖
```

## 4. UiState 设计原则

**UiState 应聚焦业务状态，避免 presentation 层承载业务逻辑推导。**

domain 层的 State 保持为纯业务状态（不含 UI 概念），但所有复杂的业务规则组合必须在 UseCase 中完成。UI 层只做简单的业务状态 → UI 表现映射。

| 做法 | 说明 | 示例 |
|------|------|------|
| **Bad Case** | UI 监听原始业务状态后自行做多条件推导 | `if (state.isLogin && state.hasPermission) view.visible = true` |
| **Good Case** | UseCase 完成业务规则组合，UI 层做简单判断 | `state.canEdit: Boolean` → `if (state.canEdit) { EditButton() }` |

**分层职责**：

| 层级 | 职责 | 示例 |
|------|------|------|
| **domain State** | 纯业务状态，不含 UI 概念 | `isLogin: Boolean`, `hasEditPermission: Boolean` |
| **domain UseCase** | 组合业务规则，输出业务结论 | `canEdit: Boolean = isLogin && hasEditPermission` |
| **presentation 层** | 简单的业务状态 → UI 表现映射 | `if (state.canEdit) { EditButton() }` |

**原则**：
- **domain 层**：所有涉及多条件组合、状态机流转、数据变换的业务规则推导必须在 UseCase 中完成。
- **presentation 层**：只允许做简单的布尔判断显示/隐藏组件、根据业务状态选择颜色/文案/样式、控制动画状态。禁止承载业务逻辑推导。
- 这样做的好处是：domain 层可通过单元测试验证完整的业务状态流转，UI 层保持薄而纯粹，无需依赖复杂逻辑即可正确渲染。

## 5. 内层 API 稳定性

在外层（presentation / app）开发时，**应尽量保持内层（domain）实现和 API 的稳定**。

- 如需修改 domain API，应先评估对外层的影响范围
- 优先通过扩展而非修改来满足外层需求
- 内层 API 变更需同步更新 `usecases.md` 及对应测试
