# ComposeDemo MVI 框架技术文档

## 1. 架构概览

本框架采用 **MVI（Model-View-Intent）** 架构模式，基于 Kotlin Coroutines + Jetpack Compose 构建。核心设计目标：

- **单向数据流**：State → UI → Event → UseCase → State
- **状态不可变**：所有 State 使用 `data class`，更新通过 `copy()`
- **业务逻辑下沉**：UseCase 承载全部业务逻辑，ViewModel 仅做生命周期桥接
- **细粒度重组**：子组件各自订阅自己的 StateFlow，避免不必要的 Compose 重组
- **嵌套状态共享**：通过 `DelegateStateHolder` 实现父子 ViewModel 之间的状态切片与双向同步

```
┌─────────────────┐     Event      ┌─────────────────┐
│   Composable    │ ─────────────→ │  BaseViewModel  │
│  (MessageArea)  │                │ (MessageViewModel)
└─────────────────┘                └────────┬────────┘
       ↑                                    │ onEvent
       │ state                              │ launch
       │ collectAsStateWithLifecycle()      ↓
       │                           ┌─────────────────┐
       │                           │   BaseUseCase   │
       │                           │ (MessageUseCase)│
       │                           └────────┬────────┘
       │                                    │ updateState
       │                                    ↓
       │                           ┌──────────────────────┐
       │                           │     StateHolder      │
       │                           │ (DelegateStateHolder)│
       │                           └────────┬─────────────┘
       │                                    │
       │         ┌──────────────────────────┼──────────────────────────┐
       │         │  childStateFlow          │  parentUpdater           │
       │         ↓                          ↓                          │
       │  ┌─────────────┐          ┌─────────────────┐                │
       └──┤   Message   │          │  StoryCardState │◄───────────────┘
          │    State    │          │  (parent)       │
          └─────────────┘          └─────────────────┘
```

---

## 2. 核心概念

### 2.1 三要素契约（UiContract）

```kotlin
// scaffold/core/.../UiState.kt
interface UiState

// scaffold/core/.../UiEvent.kt
interface UiEvent

// scaffold/core/.../UiEffect.kt
interface UiEffect
```

每个业务模块定义自己的三要素：

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
    object OnDialogueClicked : MessageEvent()
}

// biz/story/message/domain/MessageEffect.kt
sealed class MessageEffect : UiEffect
```

### 2.2 BaseEffect（通用副作用）

```kotlin
// scaffold/core/.../BaseEffect.kt
sealed interface BaseEffect : UiEffect {
    data class ShowToast(val message: String) : BaseEffect
    data class ShowDialog(val title: String, val message: String) : BaseEffect
    data object NavigateBack : BaseEffect
}
```

业务 Effect 与通用 Effect 分离：
- `Effect`：业务级副作用（如导航到详情页、打开分享面板）
- `BaseEffect`：框架级副作用（如 Toast、Dialog、返回上一页）

---

## 3. 模块分层

```
:scaffold:core          → UiState/UiEvent/UiEffect/StateHolder/BaseUseCase（纯 Kotlin，无 Android 依赖）
:scaffold:android       → BaseViewModel（AndroidX Lifecycle）
:biz:xxx:domain         → State/Event/Effect/UseCase + 单元测试（纯 Kotlin）
:biz:xxx:presentation   → ViewModel + Composable + DI Module（Android + Compose）
```

分层原则：
- `:domain` 模块**零 Android 依赖**，可直接 JVM 单元测试
- `:presentation` 模块仅负责 ViewModel 生命周期管理与 Composable UI 声明
- 业务逻辑绝对不上浮到 ViewModel

---

## 4. 核心组件详解

### 4.1 StateHolder — 状态读写最小契约

```kotlin
// scaffold/core/.../StateHolder.kt
interface StateHolder<S> {
    val state: StateFlow<S>
    fun update(transform: (S) -> S)
}

class LocalStateHolder<S>(initial: S) : StateHolder<S> {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<S> = _state.asStateFlow()
    override fun update(transform: (S) -> S) { _state.update(transform) }
}

class DelegateStateHolder<S>(
    override val state: StateFlow<S>,
    private val onUpdate: ((S) -> S) -> Unit
) : StateHolder<S> {
    override fun update(transform: (S) -> S) = onUpdate(transform)
}
```

| 实现 | 职责 |
|------|------|
| `LocalStateHolder` | 独立页面默认实现，内部持有 `MutableStateFlow` |
| `DelegateStateHolder` | 代理到外部状态，实现状态切片共享 |

### 4.2 BaseUseCase — 业务逻辑载体

```kotlin
abstract class BaseUseCase<S : UiState, E : UiEvent, F : UiEffect>(initialState: S) {
    private val _internalState = MutableStateFlow(initialState)
    private var _stateHolder: StateHolder<S>? = null

    private val activeState: StateFlow<S>
        get() = _stateHolder?.state ?: _internalState.asStateFlow()

    val state: StateFlow<S> get() = activeState

    private val _effect = Channel<F>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val _baseEffect = Channel<BaseEffect>(Channel.BUFFERED)
    val baseEffect = _baseEffect.receiveAsFlow()

    protected val currentState: S get() = activeState.value

    protected fun updateState(transform: (S) -> S) {
        _stateHolder?.update(transform) ?: _internalState.update(transform)
    }

    protected fun sendEffect(effect: F) { _effect.trySend(effect) }
    protected fun sendBaseEffect(effect: BaseEffect) { _baseEffect.trySend(effect) }

    fun bind(stateHolder: StateHolder<S>) { _stateHolder = stateHolder }

    abstract suspend fun onEvent(event: E)
}
```

**关键设计**：
- `bind(stateHolder)` 前：`updateState` 写入内部 `_internalState`（独立测试模式）
- `bind(stateHolder)` 后：`updateState` 路由到外部 `StateHolder`（共享状态模式）
- 单元测试时无需构造 ViewModel，直接实例化 UseCase 即可

### 4.3 BaseViewModel — 生命周期桥接

```kotlin
abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S,
    injectedStateHolder: StateHolder<S>? = null,
    private vararg val useCases: BaseUseCase<S, E, F>
) : ViewModel() {

    private val stateHolder: StateHolder<S> = injectedStateHolder ?: LocalStateHolder(initialState)
    val state: StateFlow<S> = stateHolder.state
    val effect: Flow<F> = merge(*useCases.map { it.effect }.toTypedArray())
    val baseEffect: Flow<BaseEffect> = merge(*useCases.map { it.baseEffect }.toTypedArray())

    init { useCases.forEach { it.bind(stateHolder) } }

    fun onEvent(event: E) {
        viewModelScope.launch { useCases.forEach { it.onEvent(event) } }
    }

    protected fun updateState(transform: (S) -> S) { stateHolder.update(transform) }

    fun <T> createDelegateStateHolder(
        childSelector: (S) -> T,
        parentUpdater: (S, T) -> S
    ): StateHolder<T> {
        val childStateFlow = state
            .map { childSelector(it) }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = childSelector(state.value)
            )

        return DelegateStateHolder(
            state = childStateFlow,
            onUpdate = { transform ->
                val childNewState = transform(childSelector(state.value))
                val parentNewState = parentUpdater(state.value, childNewState)
                updateState { parentNewState }
            }
        )
    }
}
```

**职责单一**：
- 创建/持有 `StateHolder`
- `init` 中绑定所有 UseCase
- `onEvent` 广播事件到所有 UseCase
- 暴露 `state` / `effect` / `baseEffect` 供 UI 订阅

---

## 5. 嵌套状态共享（核心特性）

### 5.1 问题场景

复杂页面（如 `StoryCardPage`）由多个独立子组件（Message/InfoBar/Input/Background）组成。每个子组件有自己的 MVI 三元组，但父页面又需要聚合所有子状态。

### 5.2 解决方案：DelegateStateHolder + stateIn 双向同步

```kotlin
// 父 ViewModel 聚合所有子状态
class StoryCardViewModel : BaseViewModel<StoryCardState, StoryCardEvent, StoryCardEffect>(
    StoryCardState(),
    null,
    StoryCardUseCase()
) {
    val messageStateHolder: StateHolder<MessageState> by lazy {
        createDelegateStateHolder(
            childSelector = StoryCardState::message,
            parentUpdater = { storyCardState, state ->
                storyCardState.copy(message = state)
            }
        )
    }
    // infoBarStateHolder, inputStateHolder, backgroundStateHolder ...
}
```

```kotlin
// 子 ViewModel 注入父提供的 StateHolder
class MessageViewModel(
    messageStateHolder: StateHolder<MessageState>,
) : BaseViewModel<MessageState, MessageEvent, MessageEffect>(
    MessageState(),
    messageStateHolder,      // ← 注入代理 StateHolder
    MessageUseCase()
)
```

### 5.3 双向同步机制

```
子 UseCase 调用 updateState
    ↓
DelegateStateHolder.onUpdate { transform }
    ├── 通过 transform(childSelector(parentState)) 计算新子状态
    └── 通过 parentUpdater(parentState, childNewState) 更新父 State
            ↓
    父 StateFlow 发射新值
            ↓
    state.map { childSelector(it) }.distinctUntilChanged().stateIn(...)
            ↓
    子 StateFlow 自动派生新值（父→子同步）
```

**为什么不会循环？**
- 子 reduce → `updateState` 更新父 → `stateIn` 派生新值 → `distinctUntilChanged` 发现值已存在 → **不发射**

**为什么没有泄漏？**
- `SharingStarted.WhileSubscribed(5000)`：当子 Composable 离开组合后 5 秒自动停止上游收集

### 5.4 状态聚合体

```kotlin
data class StoryCardState(
    val background: BackgroundState = BackgroundState(),
    val message: MessageState = MessageState(),
    val infoBar: InfoBarState = InfoBarState(),
    val input: InputState = InputState(),
) : UiState
```

父 State 是子 State 的**结构性聚合**，通过 `data class copy()` 实现不可变更新。

---

## 6. UI 层集成

### 6.1 子组件自订阅模式

每个子 Composable 内部订阅自己的 ViewModel state，实现细粒度重组隔离：

```kotlin
@Composable
fun MessageArea(
    viewModel: MessageViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Card(
        modifier = modifier.clickable {
            viewModel.onEvent(MessageEvent.OnDialogueClicked)
        }
    ) {
        // 使用 state.characterName / state.dialogueText / state.isExpanded ...
    }
}
```

**优势**：
- `InfoBarArea` 的点赞操作不会触发 `MessageArea` 重组
- 只有 `state` 真正变化的子组件才会重组

### 6.2 页面装配层

```kotlin
@Composable
fun StoryCardPage(card: StoryCard) {
    val storyViewModel: StoryCardViewModel = koinViewModel()

    val messageViewModel: MessageViewModel = koinViewModel {
        parametersOf(storyViewModel.messageStateHolder)
    }
    val infoBarViewModel: InfoBarViewModel = koinViewModel {
        parametersOf(storyViewModel.infoBarStateHolder, card.cardId)
    }
    // ...

    Box(modifier = Modifier.fillMaxSize()) {
        StoryBackground(viewModel = backgroundViewModel)
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(1f))
            MessageArea(viewModel = messageViewModel)
            InfoBarArea(viewModel = infoBarViewModel)
            InputArea(viewModel = inputViewModel)
        }
    }
}
```

**注意**：
- 父 `StoryCardPage` **不再收集** `storyViewModel.state`
- 子 ViewModel 通过 Koin 注入父提供的 `DelegateStateHolder`
- 所有状态流都是单向的：子 → 父 → `stateIn` → 子

### 6.3 Effect 收集

```kotlin
LaunchedEffect(Unit) {
    merge(messageViewModel.effect, infoBarViewModel.effect).collect { effect ->
        when (effect) {
            is StoryCardEffect.NavigateToDetail -> { /* ... */ }
        }
    }
}
```

---

## 7. DI 配置（Koin）

### 7.1 父模块

```kotlin
val storyPresentationModule = module {
    viewModel { StoryCardViewModel() }
}
```

### 7.2 子模块

```kotlin
val messagePresentationModule = module {
    viewModel { (stateHolder: StateHolder<MessageState>) ->
        MessageViewModel(messageStateHolder = stateHolder)
    }
}
```

### 7.3 注入方式

```kotlin
val messageViewModel: MessageViewModel = koinViewModel {
    parametersOf(storyViewModel.messageStateHolder)
}
```

---

## 8. 测试

### 8.1 UseCase 单元测试（纯 Kotlin，无需 Robolectric）

```kotlin
class MessageUseCaseTest {
    private val useCase = MessageUseCase()

    @Test
    fun `初始状态isExpanded为false`() {
        assertFalse(useCase.state.value.isExpanded)
    }

    @Test
    fun `点击对白切换isExpanded`() = runTest {
        useCase.onEvent(MessageEvent.OnDialogueClicked)
        assertTrue(useCase.state.value.isExpanded)
    }
}
```

**为什么可以直接测试 UseCase？**
- `BaseUseCase` 在未 `bind(stateHolder)` 前使用内部 `_internalState`
- 无需构造 ViewModel、Activity、Compose 环境

### 8.2 ViewModel 测试

```kotlin
class MessageViewModelTest {
    private val stateHolder = LocalStateHolder(MessageState())
    private val viewModel = MessageViewModel(stateHolder)

    @Test
    fun `onEvent委托给UseCase`() = runTest {
        viewModel.onEvent(MessageEvent.OnDialogueClicked)
        assertTrue(viewModel.state.value.isExpanded)
    }
}
```

---

## 9. 最佳实践

1. **State 用 data class，字段不可变（val）**
   ```kotlin
   data class MessageState(
       val isExpanded: Boolean = false,  // ✓ val
       var isExpanded: Boolean = false,  // ✗ var
   ) : UiState
   ```

2. **Event 用 sealed class，一一对应用户操作**
   ```kotlin
   sealed class MessageEvent : UiEvent {
       object OnDialogueClicked : MessageEvent()      // ✓ 单一职责
       object OnExpandClicked : MessageEvent()         // ✓ 语义明确
       object OnMessageAreaClicked : MessageEvent()    // ✗ 过于笼统
   }
   ```

3. **UseCase 处理所有业务逻辑，ViewModel 只做转发**
   ```kotlin
    // ✓ UseCase 中处理
    override suspend fun onEvent(event: MessageEvent) {
        when (event) {
            is MessageEvent.OnDialogueClicked -> {
                updateState { it.copy(isExpanded = !it.isExpanded) }
            }
        }
    }
   ```

4. **Effect 用于一次性事件，State 用于持续性数据**
   - 导航、Toast、SnackBar → `Effect`
   - 列表数据、加载状态、选中态 → `State`

5. **子组件各自订阅自己的 state，不要从父组件透传**
   ```kotlin
   // ✓ 子组件内部订阅
   val state by viewModel.state.collectAsStateWithLifecycle()

   // ✗ 父组件收集后透传
   val state by storyViewModel.state.collectAsStateWithLifecycle()
   MessageArea(state = state.message)  // 父任何字段变化都会触发 MessageArea 重组
   ```

6. **DelegateStateHolder 的 `childSelector` 只做属性访问，不做计算**
   ```kotlin
   // ✓ O(1) 属性访问
   createDelegateStateHolder(StoryCardState::message) { ... }

   // ✗ 复杂计算放 UseCase 或 State 初始化时
   createDelegateStateHolder({ it.message.toUpperCase() }) { ... }
   ```

---

## 10. 文件索引

| 模块 | 文件 | 说明 |
|------|------|------|
| `:scaffold:core` | `StateHolder.kt` | `StateHolder` 接口、`LocalStateHolder`、`DelegateStateHolder` |
| `:scaffold:core` | `BaseUseCase.kt` | UseCase 基类，状态/Effect 管理 |
| `:scaffold:core` | `UiState.kt` / `UiEvent.kt` / `UiEffect.kt` | 三要素标记接口 |
| `:scaffold:core` | `BaseEffect.kt` | 通用副作用定义 |
| `:scaffold:android` | `BaseViewModel.kt` | ViewModel 基类、`createDelegateStateHolder` |
| `:biz:story:message:domain` | `MessageState.kt` / `MessageEvent.kt` / `MessageEffect.kt` | 业务契约 |
| `:biz:story:message:domain` | `MessageUseCase.kt` | 业务逻辑 |
| `:biz:story:message:presentation` | `MessageViewModel.kt` | 子 ViewModel |
| `:biz:story:message:presentation` | `MessageArea.kt` | Compose UI |
| `:biz:story:domain` | `StoryCardState.kt` | 父状态聚合体 |
| `:biz:story:presentation` | `StoryCardViewModel.kt` | 父 ViewModel，创建子 StateHolder |
| `:biz:story:presentation` | `StoryCardPage.kt` | 页面装配层 |
