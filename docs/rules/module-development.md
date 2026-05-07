# 模块开发规范

## 1. 需求描述

每个模块的实现需求必须在 **模块根路径下的 `usecases.md`** 中进行描述。该文件是模块的功能契约，开发者和测试者都以此为依据。

```
biz/story/
├── usecases.md          ← 模块需求描述
├── domain/
│   ├── src/main/...
│   └── src/test/...     ← 功能与测试覆盖
└── presentation/
    ├── src/main/...
    └── src/androidTest/...
```

## 2. 测试覆盖要求

功能代码和测试代码都**必须覆盖 `usecases.md` 中描述的所有需求**。不允许出现需求已描述但无对应实现或测试的情况。

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

## 4. 内层 API 稳定性

在外层（presentation / app）开发时，**应尽量保持内层（domain）实现和 API 的稳定**。

- 如需修改 domain API，应先评估对外层的影响范围
- 优先通过扩展而非修改来满足外层需求
- 内层 API 变更需同步更新 `usecases.md` 及对应测试
