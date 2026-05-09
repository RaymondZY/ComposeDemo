# MVI 框架设计与实现

> 本文档详细说明 ComposeDemo 项目中自定义 MVI（Model-View-Intent）框架的设计目标、数据流、核心组件实现及状态共享机制。
> 项目模块分层与整体架构见 [overview.md](./overview.md)。

---

## 1. 设计目标

- **单向数据流**：State → UI → Event → UseCase → State
- **状态不可变**：所有 State 使用 `data class`，更新通过 `copy()`
- **业务逻辑下沉**：UseCase 承载全部业务逻辑，ViewModel 仅做生命周期桥接
- **细粒度重组**：子组件各自订阅自己的 StateFlow，避免不必要的 Compose 重组
- **嵌套状态共享**：通过 `StateHolder.derive()` 实现父子 ViewModel 之间的状态切片与双向同步

---

## 2. 数据流

```
┌─────────────────┐     sendEvent()      ┌─────────────────┐
│   Composable    │ ───────────────────→ │  BaseViewModel  │
│  (MessageArea)  │                      │ (MessageViewModel)
└─────────────────┘                      └────────┬────────┘
       ↑                                          │
       │  collectAsStateWithLifecycle()           │ receiveEvent()
       │                                          ↓
       │                               ┌─────────────────┐
       │                               │   CombineUseCase │
       │                               └────────┬────────┘
       │                                          │
       │                                          ↓ broadcast
       │                               ┌─────────────────┐
       │                               │   BaseUseCase   │
       │                               │ (MessageUseCase)│
       │                               └────────┬────────┘
       │                                          │ updateState()
       │                                          ↓
       │                               ┌──────────────────────┐
       │                               │     StateHolder      │
       │                               │ (StateHolderImpl)    │
       │                               └────────┬─────────────┘
       │                                          │
       └──────────────────────────────────────────┘
                            StateFlow 发射新值
```

---

## 3. 三要素契约

每个业务模块定义自己的三要素，均继承自 `scaffold:core` 的标记接口：

```kotlin
// scaffold/core/.../mvi/UiState.kt
interface UiState

// scaffold/core/.../mvi/UiEvent.kt
interface UiEvent

// scaffold/core/.../mvi/UiEffect.kt
interface UiEffect
```

示例：

```kotlin
// biz/story/message/domain/MessageState.kt
data class MessageState(
    val characterName: String = "",
    val characterSubtitle: String = "",
    val dialogueText: String = "",
    val isExpanded: Boolean = false,
) : UiState

// biz/story/message/domain/MessageEvent.kt
sealed class MessageEvent : UiEvent {
    data object OnDialogueClicked : MessageEvent()
}

// biz/story/message/domain/MessageEffect.kt
sealed class MessageEffect : UiEffect
```

---

## 4. BaseEffect（通用副作用）

框架级副作用与业务级副作用分离：

```kotlin
// scaffold/core/.../mvi/BaseEffect.kt
sealed interface BaseEffect : UiEffect {
    data class ShowToast(val message: String) : BaseEffect
    data class ShowDialog(val title: String, val message: String) : BaseEffect
    data object NavigateBack : BaseEffect
}
```

| 类型             | 用途                     | 收集位置                     |
|----------------|------------------------|--------------------------|
| `UiEffect` 子类型 | 业务级一次性事件（导航、打开面板）      | 按 Screen 各自收集            |
| `BaseEffect`   | 框架级事件（Toast、Dialog、返回） | `MviScreen` 统一收集，未处理会抛异常 |

---

## 5. 核心组件详解

### 5.1 StateHolder — 状态读写最小契约

```kotlin
// scaffold/core/.../mvi/StateHolder.kt
interface StateHolder<S : UiState> {
    val initialState: S
    val state: StateFlow<S>
    val currentState: S get() = state.value

    fun updateState(transform: (S) -> S)

    fun <D : UiState> derive(
        childSelector: (S) -> D,
        parentUpdater: S.(D) -> S,
    ): StateHolder<D>
}
```

| 实现                   | 职责                                              |
|----------------------|-------------------------------------------------|
| `StateHolderImpl<S>` | 默认实现，内部持有 `MutableStateFlow`，提供 `updateState()` |
| `derive()` 返回的匿名实现   | 状态切片代理，通过 `DeriveStateFlow` 实现父子双向同步            |

`DeriveStateFlow` 是自定义的 `StateFlow` 实现，基于父 `StateFlow` 的 `map { selector(it) }.distinctUntilChanged()`，保证派生子状态只在值变化时发射。

### 5.2 BaseUseCase — 业务逻辑载体

```kotlin
// scaffold/core/.../usecase/BaseUseCase.kt
abstract class BaseUseCase<S : UiState, E : UiEvent, F : UiEffect>(
    stateHolder: StateHolder<S>,
    serviceRegistry: MutableServiceRegistry,
) : MviFacade<S, E, F>, MviContext { ... }
```

**职责**：

- 接收 `StateHolder`，所有状态变更通过 `updateState { ... }` 写入
- 接收 `MutableServiceRegistry`，用于向同级 UseCase 暴露服务接口
- 通过 `EffectDispatcher` 发射业务 Effect 和 `BaseEffect`
- `onEvent(event: E)` 中处理全部业务逻辑

**单元测试时**：直接 `new UseCase(fakeStateHolder, fakeRegistry)`，无需 ViewModel、Activity、Compose 环境。

### 5.3 CombineUseCase — UseCase 聚合器

```kotlin
// scaffold/core/.../usecase/CombineUseCase.kt
class CombineUseCase<S : UiState, E : UiEvent, F : UiEffect>(
    override val stateHolder: StateHolder<S>,
    override val serviceRegistry: MutableServiceRegistry,
    vararg useCaseCreators: UseCaseFactory<S, E, F>,
) : MviFacade<S, E, F>, MviContext { ... }
```

`BaseUseCase` 与 `CombineUseCase` 均位于 `:scaffold:core` 模块中，**完全平台无关**（零 Android 依赖），可在纯 JVM 环境中实例化和测试。

`CombineUseCase` 将多个 `BaseUseCase` 聚合为一个 `MviFacade`：

- **事件广播**：`receiveEvent()` 时分发给所有 child UseCase
- **Effect 合并**：合并所有 child 的 `effect` 和 `baseEffect` Flow
- **自动注册**：每个 child UseCase 通过 `autoRegister(serviceRegistry)` 将自身实现的 `MviService` 接口注册到 Registry

### 5.4 BaseViewModel — 生命周期桥接

```kotlin
// scaffold/android/.../BaseViewModel.kt
open class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    stateHolder: StateHolder<S>,
    serviceRegistry: MutableServiceRegistry,
    vararg useCaseCreators: UseCaseFactory<S, E, F>,
) : ViewModel(), MviFacade<S, E, F>, MviContext { ... }
```

**职责单一**：

- 持有 `StateHolder`，暴露 `state: StateFlow<S>` 供 UI 订阅
- `init` 中通过 `CombineUseCase` 聚合所有 child UseCase
- `sendEvent(event)` 启动协程，将事件分发给 `CombineUseCase`
- 提供 `effect` / `baseEffect` Flow 供 Screen 级收集
- `onCleared()` 时调用 `autoUnregister` 清理 Registry

> `BaseViewModel` 位于 `:scaffold:android`，是**平台相关**的组件，继承自 `androidx.lifecycle.ViewModel`。

### 5.5 MviScreen — Screen 级 Composable 作用域

```kotlin
// scaffold/android/.../MviScreen.kt
inline fun <reified VM : BaseViewModel<*, *, *>> MviScreen(
    noinline onBaseEffect: suspend (BaseEffect) -> Boolean = { false },
    noinline parameters: (() -> ParametersHolder)? = null,
    crossinline content: @Composable (VM) -> Unit,
)
```

每个顶级 Screen 必须包裹在 `MviScreen` 中，它负责：

1. 创建独立的 **Koin Scope**（`MviKoinScopes.Screen`）
2. 创建独立的 `MutableServiceRegistryImpl`（支持 parent chain 查找）
3. 在 Scope 内实例化 ViewModel
4. 收集 `BaseEffect`；框架内置处理 `BaseEffect.ShowSnackbar`，其他 `BaseEffect` 必须被 `onBaseEffect` 处理（返回 `true`），否则抛异常

### 5.6 MviScope / MviItemScope — 嵌套作用域

```kotlin
// scaffold/android/.../MviScope.kt
@Composable
fun MviScope(scopeId: String = ..., parentRegistry: ServiceRegistry? = null, content: @Composable () -> Unit)

@Composable
fun MviItemScope(scopeId: String = ..., content: @Composable () -> Unit)
```

用于列表项（如 `VerticalPager` 中的 `StoryCardPage`）或子页面，创建比 `MviScreen` 更轻量的 Koin Scope 和 Registry。`MviItemScope` 会自动将 parent registry 传入，形成查找链。

### 5.7 screenViewModel — Scope 内 ViewModel 解析

```kotlin
// scaffold/android/.../ScreenViewModel.kt
inline fun <reified VM : BaseViewModel<*, *, *>> screenViewModel(
    key: String? = null,
    noinline parameters: (() -> ParametersHolder)? = null,
): VM
```

从当前 `LocalKoinScope` 中解析 ViewModel，key 格式为 `"${VM::class.simpleName}:$key"`。配合 `MviItemScope` 使用，确保每个列表项拥有独立的 ViewModel 实例。

---

## 6. 嵌套状态共享

### 6.1 问题场景

复杂页面（如 `StoryCardPage`）由多个独立子组件（Message / InfoBar / Input / Background）组成。每个子组件有自己的 MVI 三元组，但父页面又需要聚合所有子状态。

### 6.2 解决方案：StateHolder.derive()

父 ViewModel 通过 `derive()` 创建子状态切片，子 ViewModel 直接注入该切片：

```kotlin
// Parent ViewModel
class StoryCardViewModel(
    stateHolder: StateHolder<StoryCardState>,
    registry: MutableServiceRegistry,
) : BaseViewModel<StoryCardState, StoryCardEvent, StoryCardEffect>(
    stateHolder, registry, { holder, reg -> StoryCardUseCase(holder, reg) }
) {
    val messageStateHolder: StateHolder<MessageState> by lazy {
        stateHolder.derive(
            childSelector = StoryCardState::message,
            parentUpdater = { copy(message = it) }
        )
    }
    // infoBarStateHolder, inputStateHolder, backgroundStateHolder ...
}

// Child ViewModel 注入父提供的 StateHolder
class MessageViewModel(
    stateHolder: StateHolder<MessageState>,
    registry: MutableServiceRegistry,
) : BaseViewModel<MessageState, MessageEvent, MessageEffect>(
    stateHolder, registry, { holder, reg -> MessageUseCase(holder, reg) }
)
```

### 6.3 双向同步机制

```
子 UseCase 调用 updateState
    ↓
子 StateHolder（derive 匿名实现）的 updateState
    ├── 通过 childSelector 取出当前子状态
    ├── 通过 transform 计算新子状态
    └── 通过 parentUpdater 更新父 State
            ↓
    父 StateFlow 发射新值
            ↓
    DeriveStateFlow.collect() 中 map + distinctUntilChanged
            ↓
    子 StateFlow 自动派生新值（父→子同步）
```

**为什么不会循环？**

- 子 UseCase 调用 `updateState` 更新父 → `DeriveStateFlow` 派生新值 → `distinctUntilChanged` 发现值已存在 → **不发射**

---

## 7. 服务注册与发现（ServiceRegistry）

UseCase 之间除了共享 State 和 Effect 外，还可以通过 **ServiceRegistry** 进行显式的接口调用。

### 7.1 核心接口

```kotlin
// scaffold/core/.../spi/ServiceRegistry.kt
interface ServiceRegistry {
    fun <T : Any> find(clazz: Class<T>, tag: String? = null): T?
}

interface MutableServiceRegistry : ServiceRegistry {
    fun <T : Any> register(clazz: Class<T>, instance: T, tag: String? = null)
    fun unregister(clazz: Class<*>, tag: String? = null)
    fun unregister(instance: Any)
    fun clear()
}

interface MviService

interface TaggedMviService : MviService {
    val serviceTag: String
}
```

### 7.2 查找顺序

```
findService<Analytics>()
    ├── 同 Screen 的 registry（本地注册）
    ├── Parent Screen 的 registry（作用域链）
    └── Koin 全局容器（兜底）
```

### 7.3 自动注册

`CombineUseCase` 创建 child UseCase 后调用 `autoRegister(serviceRegistry)`，通过反射扫描 child 实现的所有 `MviService` 接口并注册到 Registry；`BaseViewModel` 也会注册自身实现的服务接口。`onCleared()` 时通过 `autoUnregister` 清理。

---

## 8. 文件索引

| 模块                  | 文件                                              | 说明                                                                     |
|---------------------|-------------------------------------------------|------------------------------------------------------------------------|
| `:scaffold:core`    | `mvi/UiState.kt` / `UiEvent.kt` / `UiEffect.kt` | 三要素标记接口                                                                |
| `:scaffold:core`    | `mvi/BaseEffect.kt`                             | 通用副作用定义（Toast、Dialog、NavigateBack）                                     |
| `:scaffold:core`    | `mvi/StateHolder.kt`                            | `StateHolder` 接口、`StateHolderImpl`、`DeriveStateFlow`、`toStateHolder()` |
| `:scaffold:core`    | `mvi/EffectDispatcher.kt`                       | Effect 分发器，基于 Channel                                                  |
| `:scaffold:core`    | `mvi/EventReceiver.kt`                          | Event 接收器，带日志打印                                                        |
| `:scaffold:core`    | `mvi/MviComponent.kt`                           | `MviComponent` 与 `MviFacade` 统一接口                                      |
| `:scaffold:core`    | `usecase/BaseUseCase.kt`                        | UseCase 基类，状态/Effect/服务注册管理                                            |
| `:scaffold:core`    | `usecase/CombineUseCase.kt`                     | UseCase 聚合器，事件广播 + Effect 合并                                           |
| `:scaffold:core`    | `spi/ServiceRegistry.kt`                        | 服务注册表读写接口                                                              |
| `:scaffold:core`    | `spi/MutableServiceRegistryImpl.kt`             | LinkedMap 实现，支持 parent chain                                           |
| `:scaffold:core`    | `spi/ServiceRegistryExt.kt`                     | `findService`/`registerService` 扩展、`autoRegister`/`autoUnregister`     |
| `:scaffold:android` | `BaseViewModel.kt`                              | ViewModel 基类，生命周期桥接                                                    |
| `:scaffold:android` | `MviScreen.kt`                                  | Screen 级 Composable，创建 Koin Scope + Registry                           |
| `:scaffold:android` | `MviScope.kt`                                   | 嵌套作用域（`MviScope`、`MviItemScope`）                                       |
| `:scaffold:android` | `ScreenViewModel.kt`                            | `screenViewModel()` 辅助函数                                               |
