# ComposeDemo 项目架构范式

本文档描述本项目采用的全局架构约定、模块划分、依赖方向与测试策略，供后续开发参考。

---

## 1. 模块结构（Multi-Module）

```
:app                    — Application 壳（Koin 初始化、Activity 路由、主题）
:biz:<feature>          — 业务特性，每个拆分为 :domain + :presentation
  :biz:todo-list
    :domain             — 纯 Kotlin/JVM，零 Android 依赖
    :presentation       — Android Library（Activity / Compose / ViewModel）
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

- `:biz:*:domain` **只能**依赖 `:service:*:api`，**禁止**依赖 `:service:*:impl`
- `:app` 是唯一拉入 `:service:*:impl` 的模块，负责在 Koin 中绑定实现
- `:biz:*:presentation` 依赖对应的 `:biz:*:domain`，可横向依赖其他 `:biz:*:presentation`

---

## 2. Clean Architecture 分层

### Domain 层（纯 Kotlin/JVM）

- **State** — 不可变数据类，描述 UI 完整状态
- **Event** — 密封类，所有用户意图与系统事件的单向入口
- **Effect** — 密封类，一次性副作用（Toast、导航、对话框）
- **UseCase** — 业务状态机，持有 `StateFlow<State>` + `Channel<Effect>`

### Presentation 层（Android Library）

- **ViewModel** — 生命周期桥接，仅将 Event 转发给 UseCase，暴露 State/Effect 供 Compose 订阅
- **Activity** — 纯容器，只负责 `setContent`，不做任何业务判断
- **Compose UI** — 无状态 Composable（`Page`）+ 有状态屏幕（`Screen`），通过 `testTag` 支持 UI 测试

---

## 3. MVI 单向数据流

```
UI Event ──► ViewModel.onEvent() ──► UseCase.onEvent()
                                              │
                    ┌──────────────────────────┘
                    ▼
              StateFlow<State>   Channel<Effect>
                    │                   │
                    ▼                   ▼
            Compose 订阅状态      LaunchedEffect 消费副作用
```

### 关键约定

1. **所有 Activity ↔ ViewModel 通信必须通过 Event** — 禁止在 Activity 中直接调用 UseCase
2. **ViewModel 只做转发** — 业务逻辑全部下沉到 UseCase
3. **Effect 是一次性的** — 用 `Channel` 或 `StateFlow<Effect?>` + `consumeEffect()` 模式
4. **State 不可变** — 只允许通过 `StateFlow.update { it.copy(...) }` 修改

---

## 4. 依赖注入（Koin + Field Injection）

本项目统一使用 **Koin Field Injection**，不采用 Constructor Injection。

```kotlin
// UseCase
class TodoUseCases : KoinComponent {
    private val checkLoginUseCase: CheckLoginUseCase by inject()
}

// ViewModel
class TodoViewModel : ViewModel(), KoinComponent {
    private val todoUseCases: TodoUseCases by inject()
}
```

### Koin Module 组织

- `:domain` 模块定义 `val xxxDomainModule = module { ... }`
- `:presentation` 模块定义 `val xxxPresentationModule = module { ... }`
- `:app` 在 `Application.onCreate` 中通过 `startKoin { modules(...) }` 统一组装

---

## 5. 服务发现（Service Discovery）

- `:service:*:api` 定义接口（如 `UserRepository`）
- `:service:*:impl` 提供生产实现（如 `MockUserRepository`）
- `:service:*:mock` 提供共享内存 Fake，供 `:domain` / `:presentation` 测试复用
- 运行时通过 Koin 绑定：`single<UserRepository> { MockUserRepository() }`

---

## 6. 测试策略

### 单元测试（JVM，`:domain` 与 `:presentation` 的 `src/test`）

| 层级 | 测试目标 | 依赖 |
|------|---------|------|
| Domain | UseCase 状态机与 Effect 输出 | 仅 `:service:*:api` + `:service:*:mock` |
| Presentation | ViewModel 桥接行为 | Koin + FakeRepository |

- **测试方法名统一使用中文描述**，不使用 `uc01_*` 或英文驼峰
- JVM 测试中通过 `startKoin { modules(...) }` 启动 Koin 上下文，注入 Fake

### UI 测试（Android Instrumentation，`src/androidTest`）

- 使用 `createComposeRule()` 测试 Compose 组件
- 通过 `testTag` 定位节点
- 关键元素必须添加 `Modifier.testTag("xxx")`

---

## 7. 技术栈

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

---

## 8. 文件与命名规范

- **测试方法**：中文描述，如 `fun 初始状态为空()`、`fun 添加Todo后状态更新并发送副作用()`
- **Koin Module**：`xxxDomainModule`、`xxxPresentationModule`，统一导出 `val xxxModules = listOf(...)`
- **Test Double**：共享 Fake 放在 `:service:*:mock`，命名 `FakeXxx`
- **Compose Tag**：`snake_case`，如 `login_username_input`、`todo_input`、`add_button`
