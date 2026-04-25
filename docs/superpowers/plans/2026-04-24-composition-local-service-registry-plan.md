# Composition-local ServiceRegistry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 MVI 框架引入 Composition-local ServiceRegistry，使 UseCase 可以通过 `findService<T>()` 在 Screen 作用域内自动发现其他 UseCase 提供的服务。

**Architecture:** `:scaffold:core` 定义纯 Kotlin 接口（`ServiceRegistry` / `ServiceProvider`），`BaseUseCase` 新增 `findService<T>()`；`:scaffold:android` 提供 Compose 集成（`LocalServiceRegistry` CompositionLocal + `screenViewModel()` + `MutableServiceRegistryImpl` 含 Koin fallback）；`:biz:story` 作为首个接入模块提供完整示例。

**Tech Stack:** Kotlin, Jetpack Compose, Koin, Coroutines

---

## Task 1: `:scaffold:core` — 核心接口与 BaseUseCase 增强

**Files:**
- Create: `scaffold/core/src/main/kotlin/zhaoyun/example/composedemo/scaffold/core/mvi/ServiceRegistry.kt`
- Modify: `scaffold/core/src/main/kotlin/zhaoyun/example/composedemo/scaffold/core/mvi/BaseUseCase.kt`

- [ ] **Step 1: 创建 `ServiceRegistry.kt`**

```kotlin
package zhaoyun.example.composedemo.scaffold.core.mvi

/**
 * 只读服务发现接口 —— UseCase 通过此接口查找服务
 */
interface ServiceRegistry {
    fun <T : Any> find(clazz: Class<T>): T?
    inline fun <reified T : Any> find(): T? = find(T::class.java)
}

/**
 * 可变服务注册表 —— 用于注册和注销服务
 */
interface MutableServiceRegistry : ServiceRegistry {
    fun <T : Any> register(clazz: Class<T>, instance: T)
    fun unregister(instance: Any)
    fun clear()

    inline fun <reified T : Any> register(instance: T) = register(T::class.java, instance)
}

/**
 * 服务提供者契约 —— UseCase 实现此接口以声明"我提供什么服务"
 */
interface ServiceProvider {
    fun provideServices(registry: MutableServiceRegistry)
}
```

- [ ] **Step 2: 修改 `BaseUseCase.kt`**

在现有 `BaseUseCase` 中新增 `serviceRegistry` 相关代码（保留原有所有代码不变）：

```kotlin
package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

abstract class BaseUseCase<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S
) {
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

    protected fun sendEffect(effect: F) {
        _effect.trySend(effect)
    }

    protected fun sendBaseEffect(effect: BaseEffect) {
        _baseEffect.trySend(effect)
    }

    /**
     * 将该 UseCase 绑定到外部 [StateHolder]（通常由 [BaseViewModel] 提供）。
     * 绑定后，所有状态读写操作都会路由到 [stateHolder]，实现多个 UseCase 共享同一份 State。
     */
    fun bind(stateHolder: StateHolder<S>) {
        _stateHolder = stateHolder
    }

    // ========== 新增：服务发现 ==========

    private var serviceRegistry: ServiceRegistry? = null

    /**
     * 由 [BaseViewModel] 在初始化时注入
     */
    fun attachServiceRegistry(registry: ServiceRegistry) {
        this.serviceRegistry = registry
    }

    /**
     * 在所在作用域内查找服务实现。
     * 找不到时抛异常，提示开发者检查是否忘记实现 [ServiceProvider]。
     */
    protected inline fun <reified T : Any> findService(): T {
        return serviceRegistry?.find<T>()
            ?: error("Service ${T::class.java.name} not found in current scope. " +
                     "Did you forget to let the providing UseCase implement ServiceProvider?")
    }

    /**
     * 在所在作用域内查找服务实现。
     * 找不到时返回 null，不抛异常。
     */
    protected inline fun <reified T : Any> findServiceOrNull(): T? {
        return serviceRegistry?.find<T>()
    }

    // ========== 新增结束 ==========

    abstract suspend fun onEvent(event: E)
}
```

- [ ] **Step 3: 编译验证 `:scaffold:core`**

Run: `./gradlew :scaffold:core:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add scaffold/core/src/main/kotlin/zhaoyun/example/composedemo/scaffold/core/mvi/
git commit -m "feat(scaffold-core): introduce ServiceRegistry and ServiceProvider interfaces, add findService to BaseUseCase"
```

---

## Task 2: `:scaffold:android` — Compose 集成与 BaseViewModel 增强

**Files:**
- Create: `scaffold/android/src/main/kotlin/zhaoyun/example/composedemo/scaffold/android/MutableServiceRegistryImpl.kt`
- Create: `scaffold/android/src/main/kotlin/zhaoyun/example/composedemo/scaffold/android/ServiceRegistryCompositionLocal.kt`
- Create: `scaffold/android/src/main/kotlin/zhaoyun/example/composedemo/scaffold/android/ScreenViewModel.kt`
- Modify: `scaffold/android/src/main/kotlin/zhaoyun/example/composedemo/scaffold/android/BaseViewModel.kt`

- [ ] **Step 1: 创建 `MutableServiceRegistryImpl.kt`**

```kotlin
package zhaoyun.example.composedemo.scaffold.android

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.error.NoBeanDefFoundException
import zhaoyun.example.composedemo.scaffold.core.mvi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.mvi.ServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.mvi.ServiceProvider

/**
 * [MutableServiceRegistry] 的默认实现。
 *
 * 查找顺序：本地注册 → Parent registry → Koin 全局容器
 */
class MutableServiceRegistryImpl(
    private val parent: ServiceRegistry? = null
) : MutableServiceRegistry, KoinComponent {

    private val services = mutableMapOf<Class<*>, Any>()

    override fun <T : Any> register(clazz: Class<T>, instance: T) {
        services[clazz] = instance
    }

    override fun unregister(instance: Any) {
        services.values.remove(instance)
    }

    override fun clear() {
        services.clear()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> find(clazz: Class<T>): T? {
        return services[clazz] as? T           // 1. 本地（同 Screen 内注册）
            ?: parent?.find(clazz)             // 2. Parent 链（父/祖父 Screen）
            ?: try { get(clazz) }              // 3. Koin 全局容器 fallback
               catch (_: NoBeanDefFoundException) { null }
    }
}
```

- [ ] **Step 2: 创建 `ServiceRegistryCompositionLocal.kt`**

```kotlin
package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import zhaoyun.example.composedemo.scaffold.core.mvi.MutableServiceRegistry

/**
 * CompositionLocal —— 向下传递当前 Screen 的 [MutableServiceRegistry]
 */
val LocalServiceRegistry = compositionLocalOf<MutableServiceRegistry?> { null }

/**
 * Screen 级别的 Registry Provider。
 *
 * 推荐所有 Screen 根 Composable 包裹此组件，以建立服务作用域。
 * 子 Screen 若也包裹 [ServiceRegistryProvider]，会自动继承父 Screen 的 registry 作为 parent。
 *
 * @param parentRegistry 显式指定 parent registry。若未指定，自动从 [LocalServiceRegistry] 获取。
 * @param content Screen 内容
 */
@Composable
fun ServiceRegistryProvider(
    parentRegistry: MutableServiceRegistry? = LocalServiceRegistry.current,
    content: @Composable () -> Unit
) {
    val registry = remember { MutableServiceRegistryImpl(parent = parentRegistry) }

    CompositionLocalProvider(LocalServiceRegistry provides registry) {
        content()
    }

    DisposableEffect(Unit) {
        onDispose { registry.clear() }
    }
}
```

- [ ] **Step 3: 创建 `ScreenViewModel.kt`**

```kotlin
package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder

/**
 * [koinViewModel] 的包装，自动将 ViewModel 注册到当前 Screen 的 [LocalServiceRegistry]。
 *
 * 所有位于 Screen 作用域内的 ViewModel 都应通过此 API 获取，
 * 而非直接使用 [koinViewModel]，以确保其 UseCase 提供的服务被自动注册。
 *
 * @param parameters Koin 参数构造器
 */
@Composable
inline fun <reified VM : BaseViewModel<*, *, *>> screenViewModel(
    noinline parameters: (ParametersHolder.() -> Unit)? = null
): VM {
    val registry = checkNotNull(LocalServiceRegistry.current) {
        "screenViewModel() must be called inside a Screen that provides LocalServiceRegistry. " +
        "Wrap your Screen root with ServiceRegistryProvider { ... }"
    }

    val viewModel = if (parameters != null) {
        koinViewModel<VM>(parameters = parameters)
    } else {
        koinViewModel<VM>()
    }

    DisposableEffect(viewModel) {
        viewModel.attachToRegistry(registry)
        onDispose { viewModel.detachFromRegistry(registry) }
    }

    return viewModel
}
```

- [ ] **Step 4: 修改 `BaseViewModel.kt`**

在现有 `BaseViewModel` 中新增 `attachToRegistry` / `detachFromRegistry` 方法（保留原有所有代码不变）：

```kotlin
package zhaoyun.example.composedemo.scaffold.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase
import zhaoyun.example.composedemo.scaffold.core.mvi.DelegateStateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.LocalStateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.mvi.ServiceProvider
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

/**
 * MVI ViewModel 基类 —— 表现层仅负责生命周期管理与平台桥接
 *
 * 所有业务逻辑已下沉到 [BaseUseCase]（位于 :domain 模块），
 * 该 ViewModel **仅**将 UI 事件广播给所有 UseCase，并暴露统一的状态流供 Compose 订阅。
 *
 * 支持一个 ViewModel 绑定多个 UseCase，它们共享同一份 [State]，并各自独立发射 [Effect]。
 *
 * ## 独立页面 vs 嵌入全局
 * - 默认情况下，内部使用 [LocalStateHolder]，ViewModel 独立管理自己的状态
 * - 可通过构造函数注入外部 [StateHolder]（如 [DelegateStateHolder]），实现状态代理到 GlobalViewModel
 * - 通过 [createDelegateStateHolder] 从父 State 中切片出子 StateHolder，实现嵌套状态共享
 *
 * ## 初始化时序说明
 * StateHolder 在构造函数中直接创建，UseCase 在 `init` 块中立即绑定到 StateHolder，
 * 无需延迟初始化。
 */
abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S,
    injectedStateHolder: StateHolder<S>? = null,
    private vararg val useCases: BaseUseCase<S, E, F>
) : ViewModel() {

    private val stateHolder: StateHolder<S> = injectedStateHolder ?: LocalStateHolder(initialState)
    val state: StateFlow<S> = stateHolder.state
    val effect: Flow<F> = merge(*useCases.map { it.effect }.toTypedArray())
    val baseEffect: Flow<BaseEffect> = merge(*useCases.map { it.baseEffect }.toTypedArray())

    init {
        useCases.forEach { it.bind(stateHolder) }
    }

    fun onEvent(event: E) {
        viewModelScope.launch {
            useCases.forEach { it.onEvent(event) }
        }
    }

    protected fun updateState(transform: (S) -> S) {
        stateHolder.update(transform)
    }

    fun <T> createDelegateStateHolder(childSelector: (S) -> T, parentUpdater: (S, T) -> S): StateHolder<T> {
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

    // ========== 新增：ServiceRegistry 注册/注销 ==========

    /**
     * 将本 ViewModel 的所有 UseCase 注册到指定 [registry]。
     *
     * 由 [screenViewModel] 自动调用，业务代码无需手动调用。
     */
    fun attachToRegistry(registry: MutableServiceRegistry) {
        useCases.forEach { it.attachServiceRegistry(registry) }
        useCases.filterIsInstance<ServiceProvider>()
              .forEach { it.provideServices(registry) }
    }

    /**
     * 从 [registry] 中注销本 ViewModel 的所有服务。
     */
    fun detachFromRegistry(registry: MutableServiceRegistry) {
        useCases.filterIsInstance<ServiceProvider>()
              .forEach { registry.unregister(it) }
    }

    // ========== 新增结束 ==========
}
```

- [ ] **Step 5: 编译验证 `:scaffold:android`**

Run: `./gradlew :scaffold:android:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add scaffold/android/src/main/kotlin/zhaoyun/example/composedemo/scaffold/android/
git commit -m "feat(scaffold-android): add Composition-local ServiceRegistry with Compose integration and Koin fallback"
```

---

## Task 3: `:biz:story:domain` — 首个接入示例（MessageAnalytics 服务）

**Files:**
- Create: `biz/story/message/domain/src/main/kotlin/zhaoyun/example/composedemo/story/message/domain/MessageAnalytics.kt`
- Modify: `biz/story/domain/src/main/kotlin/zhaoyun/example/composedemo/story/domain/StoryCardUseCase.kt`
- Modify: `biz/story/message/domain/src/main/kotlin/zhaoyun/example/composedemo/story/message/domain/MessageUseCase.kt`

- [ ] **Step 1: 创建 `MessageAnalytics.kt`**

```kotlin
package zhaoyun.example.composedemo.story.message.domain

/**
 * Message 模块提供的埋点分析服务接口。
 *
 * 由 [StoryCardUseCase] 实现并注册到 ServiceRegistry，
 * 供 [MessageUseCase] 等服务消费者发现调用。
 */
interface MessageAnalytics {
    fun trackMessageClicked(dialogueId: String)
    fun trackMessageExpanded(dialogueId: String, expanded: Boolean)
}
```

- [ ] **Step 2: 修改 `StoryCardUseCase.kt`**

```kotlin
package zhaoyun.example.composedemo.story.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase
import zhaoyun.example.composedemo.scaffold.core.mvi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.mvi.ServiceProvider
import zhaoyun.example.composedemo.story.message.domain.MessageAnalytics

class StoryCardUseCase : BaseUseCase<StoryCardState, StoryCardEvent, StoryCardEffect>(
    StoryCardState()
), ServiceProvider, MessageAnalytics {

    // 声明：本 UseCase 提供 MessageAnalytics 服务
    override fun provideServices(registry: MutableServiceRegistry) {
        registry.register<MessageAnalytics>(this)
    }

    // 实现服务接口
    override fun trackMessageClicked(dialogueId: String) {
        // TODO: 接入实际埋点 SDK
    }

    override fun trackMessageExpanded(dialogueId: String, expanded: Boolean) {
        // TODO: 接入实际埋点 SDK
    }

    override suspend fun onEvent(event: StoryCardEvent) {
        // Placeholder - business logic handled by child use-cases
    }
}
```

- [ ] **Step 3: 修改 `MessageUseCase.kt`**

```kotlin
package zhaoyun.example.composedemo.story.message.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase

class MessageUseCase : BaseUseCase<MessageState, MessageEvent, MessageEffect>(
    MessageState()
) {
    override suspend fun onEvent(event: MessageEvent) {
        when (event) {
            is MessageEvent.OnDialogueClicked -> {
                // 自动在同 Screen 的 registry 中找到 MessageAnalytics 实现
                findService<MessageAnalytics>().trackMessageClicked(event.dialogueId)
                updateState { it.copy(isExpanded = !it.isExpanded) }
            }
        }
    }
}
```

注意：`MessageEvent` 目前可能没有 `dialogueId` 字段。如果 `MessageEvent.OnDialogueClicked` 没有该字段，需要修改 `MessageEvent.kt`：

```kotlin
// biz/story/message/domain/.../MessageEvent.kt
sealed class MessageEvent : UiEvent {
    data class OnDialogueClicked(val dialogueId: String) : MessageEvent()
}
```

如果 `MessageEvent` 的定义不同，请根据实际情况调整（如使用 `object` 则不需要 `dialogueId`，埋点可以传空字符串或固定值）。

- [ ] **Step 4: 编译验证 `:biz:story:domain` 和 `:biz:story:message:domain`**

Run:
```bash
./gradlew :biz:story:domain:compileKotlin :biz:story:message:domain:compileKotlin
```
Expected: BUILD SUCCESSFUL for both modules

- [ ] **Step 5: Commit**

```bash
git add biz/story/domain/ biz/story/message/domain/
git commit -m "feat(biz-story): StoryCardUseCase provides MessageAnalytics, MessageUseCase consumes via findService"
```

---

## Task 4: `:biz:story:presentation` — StoryCardPage 使用 ServiceRegistryProvider + screenViewModel

**Files:**
- Modify: `biz/story/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/presentation/StoryCardPage.kt`

- [ ] **Step 1: 修改 `StoryCardPage.kt`**

```kotlin
package zhaoyun.example.composedemo.story.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.core.parameter.parametersOf
import zhaoyun.example.composedemo.scaffold.android.ServiceRegistryProvider
import zhaoyun.example.composedemo.scaffold.android.screenViewModel
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard
import zhaoyun.example.composedemo.story.background.presentation.BackgroundViewModel
import zhaoyun.example.composedemo.story.background.presentation.StoryBackground
import zhaoyun.example.composedemo.story.infobar.presentation.InfoBarArea
import zhaoyun.example.composedemo.story.infobar.presentation.InfoBarViewModel
import zhaoyun.example.composedemo.story.input.presentation.InputArea
import zhaoyun.example.composedemo.story.input.presentation.InputViewModel
import zhaoyun.example.composedemo.story.message.presentation.MessageArea
import zhaoyun.example.composedemo.story.message.presentation.MessageViewModel

@Composable
fun StoryCardPage(
    card: StoryCard,
) {
    // Screen 级别建立 ServiceRegistry 作用域
    ServiceRegistryProvider {

        val storyViewModel: StoryCardViewModel = screenViewModel()

        val messageViewModel: MessageViewModel = screenViewModel {
            parametersOf(storyViewModel.messageStateHolder)
        }
        val infoBarViewModel: InfoBarViewModel = screenViewModel {
            parametersOf(storyViewModel.infoBarStateHolder, card.cardId)
        }
        val inputViewModel: InputViewModel = screenViewModel {
            parametersOf(storyViewModel.inputStateHolder)
        }
        val backgroundViewModel: BackgroundViewModel = screenViewModel {
            parametersOf(storyViewModel.backgroundStateHolder)
        }

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
}
```

- [ ] **Step 2: 编译验证 `:biz:story:presentation` 及其子模块**

Run:
```bash
./gradlew :biz:story:presentation:compileDebugKotlin \
  :biz:story:message:presentation:compileDebugKotlin \
  :biz:story:input:presentation:compileDebugKotlin \
  :biz:story:infobar:presentation:compileDebugKotlin \
  :biz:story:background:presentation:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL for all five modules

- [ ] **Step 3: Commit**

```bash
git add biz/story/presentation/
git commit -m "feat(biz-story): StoryCardPage uses ServiceRegistryProvider and screenViewModel"
```

---

## Task 5: 测试与全量验证

**Files:**
- Create: `scaffold/core/src/test/kotlin/zhaoyun/example/composedemo/scaffold/core/mvi/ServiceRegistryTest.kt`
- Create: `scaffold/android/src/androidTest/kotlin/zhaoyun/example/composedemo/scaffold/android/MutableServiceRegistryImplTest.kt`（或放到 `:scaffold:core` 的 JVM test 中）

- [ ] **Step 1: 创建 `ServiceRegistryTest.kt`**

```kotlin
package zhaoyun.example.composedemo.scaffold.core.mvi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ServiceRegistryTest {

    interface TestService {
        fun doSomething(): String
    }

    class TestServiceImpl : TestService {
        override fun doSomething() = "hello"
    }

    class TestServiceProvider : ServiceProvider {
        val impl = TestServiceImpl()
        override fun provideServices(registry: MutableServiceRegistry) {
            registry.register<TestService>(impl)
        }
    }

    @Test
    fun `本地注册的服务可以被找到`() {
        val registry = MutableServiceRegistryImplForTest()
        val provider = TestServiceProvider()
        provider.provideServices(registry)

        val found = registry.find<TestService>()
        assertSame(provider.impl, found)
        assertEquals("hello", found?.doSomething())
    }

    @Test
    fun `未注册的服务返回null`() {
        val registry = MutableServiceRegistryImplForTest()
        assertNull(registry.find<TestService>())
    }

    @Test
    fun `Parent registry 作为 fallback`() {
        val parent = MutableServiceRegistryImplForTest()
        val parentImpl = TestServiceImpl()
        parent.register<TestService>(parentImpl)

        val child = MutableServiceRegistryImplForTest(parent = parent)
        val found = child.find<TestService>()

        assertSame(parentImpl, found)
    }

    @Test
    fun `本地覆盖 Parent 的服务`() {
        val parent = MutableServiceRegistryImplForTest()
        val parentImpl = TestServiceImpl()
        parent.register<TestService>(parentImpl)

        val child = MutableServiceRegistryImplForTest(parent = parent)
        val childImpl = TestServiceImpl()
        child.register<TestService>(childImpl)

        val found = child.find<TestService>()
        assertSame(childImpl, found)
    }

    @Test
    fun `UseCase findService 能找到已注册的服务`() {
        val registry = MutableServiceRegistryImplForTest()
        val provider = TestServiceProvider()
        provider.provideServices(registry)

        val useCase = object : BaseUseCase<TestState, TestEvent, TestEffect>(TestState()) {
            override suspend fun onEvent(event: TestEvent) {}
            fun testFind(): TestService = findService()
        }
        useCase.attachServiceRegistry(registry)

        assertSame(provider.impl, useCase.testFind())
    }

    @Test(expected = IllegalStateException::class)
    fun `UseCase findService 找不到时抛异常`() {
        val registry = MutableServiceRegistryImplForTest()
        val useCase = object : BaseUseCase<TestState, TestEvent, TestEffect>(TestState()) {
            override suspend fun onEvent(event: TestEvent) {}
            fun testFind(): TestService = findService()
        }
        useCase.attachServiceRegistry(registry)
        useCase.testFind()
    }

    @Test
    fun `UseCase findServiceOrNull 找不到时返回null`() {
        val registry = MutableServiceRegistryImplForTest()
        val useCase = object : BaseUseCase<TestState, TestEvent, TestEffect>(TestState()) {
            override suspend fun onEvent(event: TestEvent) {}
            fun testFind(): TestService? = findServiceOrNull()
        }
        useCase.attachServiceRegistry(registry)

        assertNull(useCase.testFind())
    }

    // 简化测试用的 MutableServiceRegistry 实现（无 Koin fallback）
    private class MutableServiceRegistryImplForTest(
        private val parent: ServiceRegistry? = null
    ) : MutableServiceRegistry {
        private val services = mutableMapOf<Class<*>, Any>()

        override fun <T : Any> register(clazz: Class<T>, instance: T) {
            services[clazz] = instance
        }

        override fun unregister(instance: Any) {
            services.values.remove(instance)
        }

        override fun clear() {
            services.clear()
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> find(clazz: Class<T>): T? {
            return services[clazz] as? T ?: parent?.find(clazz)
        }
    }

    private data class TestState(val value: Int = 0) : UiState
    private object TestEvent : UiEvent
    private object TestEffect : UiEffect
}
```

- [ ] **Step 2: 运行 `:scaffold:core` 测试**

Run: `./gradlew :scaffold:core:test`
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 3: 全量编译与全量测试**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add scaffold/core/src/test/
git commit -m "test(scaffold-core): add ServiceRegistry unit tests"
```

---

## Task 6: 文档更新

**Files:**
- Modify: `MVI_FRAMEWORK.md`

- [ ] **Step 1: 在 `MVI_FRAMEWORK.md` 中新增"UseCase 间服务发现"章节**

在现有章节 4（核心组件详解）之后新增 4.4 节，或在章节 5（嵌套状态共享）之后新增章节。

推荐在 4.3 BaseViewModel 之后新增 **4.4 ServiceRegistry — UseCase 间服务发现**：

```markdown
### 4.4 ServiceRegistry — UseCase 间服务发现

除了通过共享 [StateHolder] 读写状态、通过 [Effect] 发射副作用之外，
UseCase 之间还可以通过 **ServiceRegistry** 进行显式的接口调用。

**适用场景：**
- UseCase A 需要调用 UseCase B 的某个业务方法（如埋点、鉴权）
- 比共享状态更直接，比 Effect 更同步

**核心接口：**

```kotlin
interface ServiceRegistry {
    fun <T : Any> find(clazz: Class<T>): T?
}

interface ServiceProvider {
    fun provideServices(registry: MutableServiceRegistry)
}
```

**使用方式：**

1. **提供服务：** UseCase 实现 `ServiceProvider`，在 `provideServices` 中注册自己
2. **消费服务：** UseCase 调用 `findService<T>()` 自动发现同 Screen 内的实现

**查找顺序：**

```
findService<Analytics>()
    ├── 同 Screen 的 registry（本地注册）
    ├── Parent Screen 的 registry（作用域链）
    └── Koin 全局容器（兜底）
```

**Screen 级别集成：**

```kotlin
@Composable
fun StoryCardPage(card: StoryCard) {
    ServiceRegistryProvider {
        val storyVm = screenViewModel<StoryCardViewModel>()
        val messageVm = screenViewModel<MessageViewModel> {
            parametersOf(storyVm.messageStateHolder)
        }
        // ...
    }
}
```

**向后兼容：**
未实现 `ServiceProvider` 的 UseCase 不受影响；未使用 `ServiceRegistryProvider` 的 Screen 正常工作。
```

- [ ] **Step 2: 提交**

```bash
git add MVI_FRAMEWORK.md
git commit -m "docs: add ServiceRegistry chapter to MVI_FRAMEWORK.md"
```

---

## Self-Review Checklist

- [ ] **Spec coverage:** 设计文档中所有需求（核心接口、Compose 集成、Koin fallback、作用域链、使用示例、向后兼容）均有对应任务
- [ ] **Placeholder scan:** 无 TBD/TODO/"implement later"
- [ ] **Type consistency:** `ServiceRegistry` / `MutableServiceRegistry` / `ServiceProvider` / `findService<T>()` / `screenViewModel()` 在所有任务中定义和使用一致
- [ ] **File paths:** 所有路径基于实际项目结构
- [ ] **Commit sequence:** Task 1 → Task 2 → Task 3 → Task 4 → Task 5 → Task 6，每步可独立编译验证
