# ComposeDemo 项目架构范式

本文档描述本项目采用的全局架构约定、模块划分、依赖方向与测试策略，供后续开发参考。

---

## 1. 模块结构（Multi-Module）

```
:app                    — Application 壳（Koin 初始化、Navigation Host、主题）
:core:common            — 纯 Kotlin/JVM，通用 MVI 契约与跨模块复用组件
:biz:<feature>          — 业务特性，每个拆分为 :domain + :presentation
  :biz:todo-list
    :domain             — 纯 Kotlin/JVM，零 Android 依赖，零 DI 框架依赖
    :presentation       — Android Library（Compose / ViewModel）
  :biz:login
    :domain
    :presentation
:service:<name>         — 横向服务，拆分为 :api / :impl / :mock
  :service:user-center
    :api                — 接口与数据模型
    :impl               — 生产实现（MockUserRepository）
    :mock               — 共享内存 Fake，供测试复用
```

### 依赖方向铁律

- `:biz:*:domain` **只能**依赖 `:service:*:api` 与 `:core:common`，**禁止**依赖 `:service:*:impl` 与任何 DI 框架
- `:app` 是唯一拉入 `:service:*:impl` 的模块，负责在 Koin 中绑定实现
- `:biz:*:presentation` **禁止**横向依赖其他 `:biz:*:presentation`，跨模块通信通过 `:app` 层路由或回调接口解耦

---

## 2. Clean Architecture 分层

### Domain 层（纯 Kotlin/JVM）

- **State** — 不可变数据类，描述 UI 完整状态
- **Event** — 密封类，所有用户意图与系统事件的单向入口
- **Effect** — 通用 `UiEffect` 密封类（定义于 `:core:common`），一次性副作用（Toast、导航、对话框）
- **UseCase** — 业务状态机，持有 `StateFlow<State>` + `Channel<UiEffect>`

### Presentation 层（Android Library）

- **ViewModel** — 生命周期桥接，仅将 Event 转发给 UseCase，暴露 State/Effect 供 Compose 订阅
- **Compose UI** — 无状态 Composable（`Page`）+ 有状态屏幕（`Screen`），通过 `testTag` 支持 UI 测试
- **Activity** — 单 Activity 架构，仅 `:app` 模块持有 Activity，业务模块只暴露 Composable

---

## 3. MVI 单向数据流

```
UI Event ──► ViewModel.onEvent() ──► UseCase.onEvent()
                                              │
                    ┌──────────────────────────┘
                    ▼
              StateFlow<State>   Channel<UiEffect>
                    │                   │
                    ▼                   ▼
            Compose 订阅状态      LaunchedEffect 消费副作用
```

### 关键约定

1. **所有页面间通信必须通过 Navigation 或回调接口** — 禁止在 Composable 中直接引用其他模块的 Activity
2. **ViewModel 只做转发** — 业务逻辑全部下沉到 UseCase
3. **Effect 是一次性的** — 统一使用 `Channel<UiEffect>` + `receiveAsFlow()`，禁止 `StateFlow<Effect?>` + `consumeEffect()` 模式
4. **State 不可变** — 只允许通过 `StateFlow.update { it.copy(...) }` 修改

---

## 4. 依赖注入（Koin + Constructor Injection）

本项目统一使用 **Koin Constructor Injection**，Domain 层零 Koin 依赖。

```kotlin
// UseCase（纯 Kotlin，无 KoinComponent）
class TodoUseCases(
    private val checkLoginUseCase: CheckLoginUseCase
) { ... }

// ViewModel
class TodoViewModel(
    private val todoUseCases: TodoUseCases
) : ViewModel() { ... }

// Koin Module
val todoPresentationModule = module {
    viewModel { TodoViewModel(get()) }
}
```

### Koin Module 组织

- `:domain` 模块定义 `val xxxDomainModule = module { ... }`
- `:presentation` 模块定义 `val xxxPresentationModule = module { ... }`，并聚合 `xxxDomainModule` 导出 `val xxxModules = listOf(...)`
- `:app` 在 `Application.onCreate` 中通过 `startKoin { modules(...) }` 统一组装

---

## 5. 服务发现（Service Discovery）

- `:service:*:api` 定义接口（如 `UserRepository`）
- `:service:*:impl` 提供生产实现（如 `MockUserRepository`）
- `:service:*:mock` 提供共享内存 Fake，供 `:domain` / `:presentation` 测试复用
- 运行时通过 Koin 绑定：`single<UserRepository> { MockUserRepository() }`

---

## 6. 导航（单 Activity + Navigation Compose）

- `:app` 模块持有唯一 `MainActivity`，内部使用 `NavHost` 管理路由
- 业务模块只暴露 `@Composable` 页面与回调接口（如 `onLoginSuccess`、`onNavigateToLogin`）
- 禁止多 Activity 架构，禁止业务模块持有 Activity

---

## 7. 测试策略

### 单元测试（JVM，`:domain` 与 `:presentation` 的 `src/test`）

| 层级 | 测试目标 | 依赖 |
|------|---------|------|
| Domain | UseCase 状态机与 Effect 输出 | 仅 `:service:*:api` + `:service:*:mock` + `:core:common` |
| Presentation | ViewModel 桥接行为 | 直接构造注入依赖，无需启动 Koin |

- **测试方法名统一使用中文描述**，不使用 `uc01_*` 或英文驼峰
- Domain 层测试直接传入 Fake 实例，**无需启动 Koin 容器**
- UI 测试（`src/androidTest`）通过 `startKoin { modules(xxxModules) }` 启动完整模块并覆盖 Repository

### UI 测试（Android Instrumentation，`src/androidTest`）

- 使用 `createComposeRule()` 测试 Compose 组件
- 通过 `testTag` 定位节点
- 关键元素必须添加 `Modifier.testTag("xxx")`

---

## 8. 技术栈

| 组件 | 版本 |
|------|------|
| Gradle | 8.13 |
| AGP | 8.13.2 |
| Kotlin | 2.0.21 |
| Compile SDK | 36 |
| Min SDK | 26 |
| Java / Kotlin Target | 11 |
| Coroutines | 1.9.0 |
| Koin | 3.5.6 |
| Compose BOM | 2024.09.00 |
| Navigation Compose | 2.8.0 |

---

## 9. 文件与命名规范

- **测试方法**：中文描述，如 `fun 初始状态为空()`、`fun 添加Todo后状态更新并发送副作用()`
- **Koin Module**：`xxxDomainModule`、`xxxPresentationModule`，统一导出 `val xxxModules = listOf(...)`
- **Test Double**：共享 Fake 放在 `:service:*:mock`，命名 `FakeXxx`
- **Compose Tag**：`snake_case`，如 `login_username_input`、`todo_input`、`add_button`
