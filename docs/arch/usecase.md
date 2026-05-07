# UseCase 开发与测试

> 本文档说明 ComposeDemo 项目中 UseCase 的编写规范与测试策略。MVI 框架的 UseCase 设计详见 [mvi.md](./mvi.md)，项目整体架构见 [overview.md](./overview.md)。

---

## 1. UseCase 编写规范

### 1.1 职责边界

- **UseCase 承载全部业务逻辑**：状态变更、网络请求、本地存储、Effect 发射
- **ViewModel 不做业务判断**：仅作为生命周期桥接，将 Event 转发给 UseCase
- **State 不可变**：所有状态更新通过 `updateState { copy(...) }` 完成

### 1.2 典型结构

```kotlin
class MessageUseCase(
    stateHolder: StateHolder<MessageState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<MessageState, MessageEvent, MessageEffect>(
    stateHolder, serviceRegistry
) {
    override fun onEvent(event: MessageEvent) {
        when (event) {
            is MessageEvent.OnDialogueClicked -> {
                updateState { copy(isExpanded = !isExpanded) }
            }
            // ...
        }
    }
}
```

### 1.3 服务发现

UseCase 可通过 `findService<T>()` 查找同级 UseCase 暴露的 `MviService` 接口：

```kotlin
val analytics = findService<Analytics>()
analytics?.track("message_click")
```

自动注册机制会在 `init` 时扫描当前 UseCase 实现的所有 `MviService` 接口并注册到 Registry（详见 [mvi.md §7.3](./mvi.md)）。

---

## 2. 测试策略

### 2.1 平台无关代码测试（JUnit，JVM 快速运行）

`:scaffold:core`、`:biz:*:domain`、`:service:*:api` 中的代码均为**平台无关**，使用标准 JUnit 测试即可，无需 Robolectric 或模拟器。

#### UseCase 单元测试

```kotlin
class MessageUseCaseTest {
    @Test
    fun `点击对白切换isExpanded`() = runTest {
        val useCase = MessageUseCase(
            MessageState().toStateHolder(),
            FakeMutableServiceRegistry()
        )
        useCase.receiveEvent(MessageEvent.OnDialogueClicked)
        assertTrue(useCase.currentState.isExpanded)
    }
}
```

**测试优势**：

- 零 Android 依赖，JVM 直接运行
- 无需 ViewModel、Activity、Compose 环境
- 可快速验证业务逻辑正确性

#### StateHolder / DeriveStateFlow 测试

同样为纯 Kotlin 测试，可直接验证 `derive()` 的双向同步机制。

---

### 2.2 平台相关代码测试（JUnit + androidTest）

`:scaffold:android`、`:biz:*:presentation`、`:app` 中的代码**依赖 Android 平台**，需结合两种测试方式：

#### ViewModel 测试（JUnit）

```kotlin
class BaseViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `sendEvent委托给UseCase`() = runTest {
        val stateHolder = DemoState().toStateHolder()
        val viewModel = DemoViewModel(stateHolder, MutableServiceRegistryImpl())
        viewModel.sendEvent(DemoEvent.Increment)
        advanceUntilIdle()
        assertEquals(1, viewModel.state.value.count)
    }
}
```

#### Compose UI 仪器测试（androidTest）

使用 `ComposeTestRule` 测试 Composable 交互，需运行在模拟器或真机上：

- `scaffold/android/src/androidTest/.../ServiceRegistryComposeTest.kt`
- `scaffold/android/src/androidTest/.../ScreenViewModelTest.kt`

---

## 3. 测试工具类

| 工具类                          | 用途                     | 位置                                     |
|------------------------------|------------------------|----------------------------------------|
| `FakeMutableServiceRegistry` | 测试用的 Registry 假实现      | `scaffold/core/src/test/...`           |
| `toStateHolder()`            | 将 State 转为 StateHolder | `scaffold/core/.../mvi/StateHolder.kt` |
| `MainDispatcherRule`         | 统一调度器到 TestDispatcher  | 测试模块                                   |
