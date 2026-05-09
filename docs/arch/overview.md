# ComposeDemo 项目架构说明

## 1. 项目概览

ComposeDemo 是一个基于 **Jetpack Compose** + **Kotlin Coroutines** 构建的 Android 演示项目，采用自定义的 **MVI（Model-View-Intent）** 框架实现单向数据流。

核心设计目标：

- **单向数据流**：State → UI → Event → UseCase → State
- **状态不可变**：所有 State 使用 `data class`，更新通过 `copy()`
- **业务逻辑下沉**：UseCase 承载全部业务逻辑，ViewModel 仅做生命周期桥接
- **细粒度重组**：子组件各自订阅自己的 StateFlow，避免不必要的 Compose 重组
- **嵌套状态共享**：通过 `StateHolder.derive()` 实现父子 ViewModel 之间的状态切片与双向同步

> MVI 框架的详细设计、数据流、核心组件实现及状态共享机制，请参见 [mvi.md](./mvi.md)。

---

## 2. 模块分层

项目采用多模块 Gradle 结构。当前 `settings.gradle.kts` 中包含 31 个 `include`，实际模块清单以该文件为准；架构上按职责分为以下几层：

```
:app                          — Application 入口（Koin 初始化、Activity）

:scaffold:core               — 纯 Kotlin MVI 抽象层（无 Android 依赖）
  ├─ mvi/                    — UiState / UiEvent / UiEffect / StateHolder / EffectDispatcher
  ├─ usecase/                — BaseUseCase / CombineUseCase
  ├─ spi/                    — ServiceRegistry（UseCase 间服务发现）
  └─ context/                — MviContext / MviLogger

:scaffold:android            — Android/Compose 绑定层
  ├─ BaseViewModel.kt        — ViewModel 基类
  ├─ MviScreen.kt            — Screen 级别 Composable 作用域
  ├─ MviScope.kt             — 嵌套作用域（列表项、子页面）
  └─ ScreenViewModel.kt      — Koin Scope 内 ViewModel 解析辅助

:service:xxx:api             — 业务服务接口 + 数据模型
:service:xxx:impl            — 真实实现（当前部分为 mock）
:service:xxx:mock            — 测试用的 Fake 实现

:biz:xxx:domain              — State / Event / Effect / UseCase（纯 Kotlin，零 Android 依赖）
:biz:xxx:presentation        — ViewModel + Composable + Koin DI Module（平台相关）
```

当前主要业务模块包括 `home`、`feed`、`story`，其中 `story` 下继续拆分为 `message`、`infobar`、`input`、`background`、`comment-panel`、`share-panel`、`story-panel` 等子模块。

### 平台无关 vs 平台相关

| 类型       | 模块                                                | 说明                                | 测试方式                  |
|----------|---------------------------------------------------|-----------------------------------|-----------------------|
| **平台无关** | `:scaffold:core`、`:biz:*:domain`、`:service:*:api` | 纯 Kotlin，零 Android 依赖             | JUnit（JVM 快速运行）       |
| **平台相关** | `:scaffold:android`、`:biz:*:presentation`、`:app`  | 依赖 AndroidX / Compose / ViewModel | JUnit + `androidTest` |

`BaseUseCase` 与 `CombineUseCase` 位于 `:scaffold:core` 中，是平台无关的 MVI 框架核心，可在纯 JVM 环境中测试。

### 分层原则

| 层级                    | 职责                                    | 依赖规则                                                           |
|-----------------------|---------------------------------------|----------------------------------------------------------------|
| `:scaffold:core`      | MVI 契约、状态管理、Effect 分发、UseCase 聚合      | 无 Android 依赖                                                   |
| `:scaffold:android`   | ViewModel 生命周期、Compose 作用域、Koin 集成    | 仅依赖 AndroidX + `:scaffold:core`                                |
| `:service:*`          | 应用声明周期的服务                             | 可依赖 `:scaffold:core`                                           |
| `:biz:*:domain`       | 业务逻辑：UseCase、State、Event、Effect（平台无关） | 仅依赖 `:scaffold:core` + `:service:*:api`                        |
| `:biz:*:presentation` | UI 层：ViewModel、Composable、DI 模块（平台相关） | 可依赖 AndroidX + Compose + `:scaffold:android` + `:biz:*:domain` |
| `:app`                | 应用组装：Koin 启动、模块注册、Activity            | 依赖所有 `:biz:*:presentation`                                     |

---

## 3. MVI 框架概览

本项目采用自定义 MVI 框架实现 UI 与业务逻辑的单向数据流。以下仅为概览，详细实现参见 [mvi.md](./mvi.md)。

### 3.1 数据流

```
Composable ──sendEvent()──→ BaseViewModel ──receiveEvent()──→ CombineUseCase ──broadcast──→ BaseUseCase
    ↑                                                                                              │
    └──────────────────── collectAsStateWithLifecycle() ←── StateFlow ←── StateHolder ←── updateState()
```

### 3.2 核心组件

| 组件                | 职责                                               | 所在模块                |
|-------------------|--------------------------------------------------|---------------------|
| `StateHolder`     | 状态读写最小契约，支持 `derive()` 状态切片                      | `:scaffold:core`    |
| `BaseUseCase`     | 业务逻辑载体，处理 Event 并更新 State                        | `:scaffold:core`    |
| `CombineUseCase`  | 聚合多个 UseCase，事件广播 + Effect 合并                    | `:scaffold:core`    |
| `BaseViewModel`   | ViewModel 生命周期桥接，持有 StateHolder                  | `:scaffold:android` |
| `MviScreen`       | Screen 级 Composable 作用域，创建 Koin Scope + Registry | `:scaffold:android` |
| `ServiceRegistry` | UseCase 间服务发现，支持 parent chain 查找                 | `:scaffold:core`    |

### 3.3 嵌套状态共享

复杂页面（如 `StoryCardPage`）由多个独立子组件组成。父 ViewModel 通过 `StateHolder.derive()` 创建子状态切片，子 ViewModel 注入该切片，实现父子状态的双向同步。详见 [mvi.md §6](./mvi.md)。

---

## 4. UI 层集成

### 4.1 子组件自订阅模式

每个子 Composable 内部订阅自己的 ViewModel state，实现细粒度重组隔离：

```kotlin
@Composable
fun MessageArea(viewModel: MessageViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.clickable {
            viewModel.sendEvent(MessageEvent.OnDialogueClicked)
        }
    ) {
        // 使用 state.characterName / state.dialogueText / state.isExpanded ...
    }
}
```

**优势**：`InfoBarArea` 的点赞操作不会触发 `MessageArea` 重组，只有真正变化的子组件才会重组。

### 4.2 页面装配层（StoryCardPage）

```kotlin
@Composable
fun StoryCardPage(viewModel: StoryCardViewModel, card: StoryCard) {
    val messageViewModel: MessageViewModel = screenViewModel(card.cardId) {
        parametersOf(viewModel.messageStateHolder)
    }
    val infoBarViewModel: InfoBarViewModel = screenViewModel(card.cardId) {
        parametersOf(card.cardId, viewModel.infoBarStateHolder)
    }
    // ...

    Box(modifier = Modifier.fillMaxSize()) {
        StoryBackground(viewModel = backgroundViewModel)
        Column(modifier = Modifier.fillMaxSize()) {
            MessageArea(viewModel = messageViewModel)
            InfoBarArea(viewModel = infoBarViewModel)
            InputArea(viewModel = inputViewModel)
        }
    }
}
```

注意：

- 父 `StoryCardPage` **不再收集** `storyViewModel.state`
- 子 ViewModel 通过 `screenViewModel(key)` + `parametersOf(stateHolder)` 注入父提供的切片

### 4.3 Effect 收集

```kotlin
LaunchedEffect(viewModel) {
    viewModel.effect.collect { effect ->
        when (effect) {
            is StoryCardEffect.NavigateToDetail -> { /* ... */
            }
        }
    }
}
```

---

## 5. DI 配置

项目使用 **Koin** 作为依赖注入框架。所有模块在 `:app` 中统一注册，子 ViewModel 通过带参数的 factory 注入父提供的 `StateHolder`。详见 [di.md](./di.md)。

---

## 6. 测试策略

- **UseCase 单元测试**：纯 Kotlin，零 Android 依赖，JVM 直接运行
- **ViewModel 测试**：验证生命周期桥接与事件分发
- **Android 仪器测试**：Compose UI 交互测试

详见 [usecase.md](./usecase.md)。

---

## 7. 相关文档

| 文档                         | 内容                   |
|----------------------------|----------------------|
| [mvi.md](./mvi.md)         | MVI 框架详细设计与实现、核心文件索引 |
| [di.md](./di.md)           | Koin DI 配置、模块注册清单    |
| [usecase.md](./usecase.md) | UseCase 编写规范与测试策略    |
