# Agent 指南

本文件为 AI Agent 提供项目背景与文档入口指引。

## 项目简介

ComposeDemo 是一个基于 **Jetpack Compose** + **Kotlin Coroutines** 的 Android 演示项目，采用自定义 **MVI（Model-View-Intent）** 框架实现单向数据流。

- 核心框架位于 `:scaffold:core`（纯 Kotlin，零 Android 依赖）
- Android 绑定层位于 `:scaffold:android`
- 业务模块按 `:biz:xxx:domain`（平台无关）+ `:biz:xxx:presentation`（平台相关）拆分

## 文档结构

所有项目文档统一存放在 `docs/` 下，按类型分目录：

| 目录             | 内容      | 关键文件                                                                                                 |
|----------------|---------|------------------------------------------------------------------------------------------------------|
| `docs/arch/`   | 架构设计文档  | `overview.md`（项目架构总览）、`mvi.md`（MVI 框架实现）、`di.md`（DI 配置）、`usecase.md`（UseCase 与测试）                    |
| `docs/rules/`  | 项目规范与规则 | `module-development.md`（开发顺序与规范）、`environment.md`（JDK/Gradle 版本）、`documentation_location.md`（文档存放规则） |
| `docs/skills/` | 技能与工具说明 | `doc_sync`（文档同步技能）                                                                                   |

**Agent 应先阅读 `docs/arch/overview.md` 了解整体架构，再根据任务深入对应子文档。**

## 构建与测试

```bash
# 构建
./gradlew assembleDebug

# 运行所有单元测试
./gradlew test

# 单模块测试
./gradlew :scaffold:core:test
./gradlew :biz:story:message:domain:test

# Android 仪器测试
./gradlew connectedAndroidTest

# Lint
./gradlew lint
```

> ⚠️ 必须使用 **JDK 21** 构建。详见 [docs/rules/environment.md](docs/rules/environment.md)。

## 核心开发原则

- **从内向外开发**：先完成 `:biz:xxx:domain` 层（UseCase + JUnit 测试），再到 `:biz:xxx:presentation` 层（ViewModel/Composable + JUnit/androidTest）
- **平台无关层优先**：`:scaffold:core`、`:biz:*:domain`、`:service:*:api` 为零 Android 依赖的纯 Kotlin 代码，使用标准 JUnit 测试
- **内层 API 稳定**：外层开发时尽量保持 domain 层实现和 API 不变
- **需求文档化**：各子层（domain / presentation）按需放置 `usecases.md` 描述需求，功能与测试必须全部覆盖
