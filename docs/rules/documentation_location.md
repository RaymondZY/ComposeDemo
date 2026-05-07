# 文档存放规范

项目中的所有文档必须统一放置在 `docs/` 目录下，按类型划分到对应子目录中。该规范与当前使用的 CLI 工具或 IDE 无关，所有开发者都应遵循。

## 目录结构

```
docs/
├── arch/                    — 架构文档
│   ├── overview.md          — 项目架构总览
│   ├── mvi.md               — MVI 框架详细设计
│   ├── di.md                — DI 配置说明
│   └── usecase.md           — UseCase 开发与测试
├── rules/                   — 项目规范与规则
│   ├── module-development.md — 模块开发规范
│   ├── environment.md        — 开发环境要求
│   └── documentation_location.md — 本文档
└── skills/                  — 技能与工具说明（项目级）
```

## 存放原则

| 文档类型 | 存放位置           | 说明                         |
|------|----------------|----------------------------|
| 架构设计 | `docs/arch/`   | 项目整体架构、模块分层、框架实现、技术方案      |
| 开发规范 | `docs/rules/`  | 编码规范、开发流程、环境要求、文档存放规则      |
| 技能工具 | `docs/skills/` | 特定工具/技能的使用说明、最佳实践、CLI 技能定义 |

## Skills 存放规则（强制）

**所有 CLI 创建或维护的 skills，默认必须放到项目路径下的 `docs/skills/` 中。**

- 每个 skill 一个子目录，目录名与 skill 名一致，内部包含 `SKILL.md`：
  ```
  docs/skills/
  └── doc-sync/
      └── SKILL.md
  ```
- **禁止**将项目相关的 skills 放到用户级路径（如 `~/.agents/skills/`）或其他位置。
- 项目级 skills 随代码仓库一起版本控制，确保团队成员共享同一套技能定义。
- 仅在 skill 明确为跨项目通用、且与当前项目无关时，才可考虑用户级存放；否则一律项目级。

## 注意事项

- 文档不依赖特定的 CLI 工具或 IDE，任何开发者通过文件系统或版本控制均可访问。
- 新增文档时，先判断文档类型，再放到对应子目录中。
- 如需在 Markdown 中引用其他文档，使用相对路径（如 `[overview.md](../arch/overview.md)`）。
- 新增或修改 skill 后，应同步更新 `docs/skills/` 下的对应文件，确保文档与技能定义一致。
