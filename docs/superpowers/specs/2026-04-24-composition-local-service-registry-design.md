# Composition-local ServiceRegistry 设计文档

## 背景

当前 MVI 框架中，同一个 `BaseViewModel` 可以绑定多个 `BaseUseCase`，它们通过**共享同一个 `StateHolder`** 实现隐式通信。当 UseCase 之间需要更**显式**、更**类型安全**地调用彼此的能力时，现有机制不够直观。

目标是为 UseCase 引入一种**服务发现（Service Discovery）**机制：UseCase 无需通过构造函数显式注入依赖，而是像"服务发现"一样在所在作用域内自动找到需要的接口实现。

## 核心洞察：Compose 组合树即作用域

Jetpack Compose 的组合树（Composition Tree）天然具备层级和生命周期：
- 父 Composable 可以通过 `CompositionLocalProvider` 向下传递值
- 子 Composable 通过 `CompositionLocal.current` 读取
- `remember` 状态跟随 Composable 生命周期
- `DisposableEffect` 可在销毁时清理

利用这一机制，可以将 **Screen（页面）** 定义为服务作用域的边界：
- Screen 进入时创建 `ServiceRegistry`
- Screen 内的所有 ViewModel 自动注册其 UseCase 提供的服务
- Screen 退出时统一销毁 registry，所有服务自动解除
- 子 Screen 可以继承父 Screen 的 registry，形成作用域链

## 目标

1. UseCase 通过 `findService<T>()` 一行代码发现同 Screen 内其他 UseCase 提供的服务
2. 服务注册**自动化** —— ViewModel 加入 Screen 时自动注册，无需手动 `registry.register()`
3. 作用域跟随 Compose Screen 生命周期，Screen 退出自动清理
4. 支持作用域链 —— 子 Screen 可以访问父 Screen 注册的服务
5. 兼容现有代码 —— 不强制改造已有 UseCase，增量接入

## 非目标

- 不替换 Koin 全局 DI —— Koin 仍负责跨模块/跨 Screen 的依赖注入
- 不引入运行时反射扫描 —— 服务通过显式 `ServiceProvider` 接口声明
- 不改写现有 `StateHolder` / MVI 三要素 —— 服务发现是独立机制，与状态流并行存在

## 架构设计

### 模块分层

```
:scaffold:core          → ServiceRegistry / MutableServiceRegistry / ServiceProvider（纯 Kotlin）
:scaffold:android       → LocalServiceRegistry CompositionLocal + screenViewModel() + KoinServiceRegistry
:biz:xxx:domain         → UseCase 实现 ServiceProvider / 调用 findService<T>()
:biz:xxx:presentation   → Screen Composable 提供 registry，ViewModel 通过 screenViewModel() 获取
```

### 1. 核心接口（`:scaffold:core`）

```kotlin
// ServiceRegistry.kt
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

### 2. BaseUseCase 增强

```kotlin
// BaseUseCase.kt（新增部分）
abstract class BaseUseCase<S : UiState, E : UiEvent, F : UiEffect>(initialState: S) {
    // ... 原有代码不变 ...

    private var serviceRegistry: ServiceRegistry? = null

    /**
     * 由 BaseViewModel 在初始化时注入
     */
    fun attachServiceRegistry(registry: ServiceRegistry) {
        this.serviceRegistry = registry
    }

    /**
     * 在所在作用域内查找服务实现
     * 查找顺序：同 Screen registry → parent registry → Koin 全局（通过实现层 fallback）
     */
    protected inline fun <reified T : Any> findService(): T {
        return serviceRegistry?.find<T>()
            ?: error("Service ${T::class.java.name} not found in current scope. " +
                     "Did you forget to let the providing UseCase implement ServiceProvider?")
    }

    /**
     * 安全查找：找不到时返回 null，不抛异常
     */
    protected inline fun <reified T : Any> findServiceOrNull(): T? {
        return serviceRegistry?.find<T>()
    }
}
```

### 3. BaseViewModel 增强

```kotlin
// BaseViewModel.kt（新增部分）
abstract class BaseViewModel<...>(...) : ViewModel() {
    // ... 原有代码不变 ...

    /**
     * 将本 ViewModel 的所有 UseCase 注册到指定 registry
     * 由 screenViewModel() 自动调用，业务代码无需手动调用
     */
    fun attachToRegistry(registry: MutableServiceRegistry) {
        useCases.forEach { it.attachServiceRegistry(registry) }
        useCases.filterIsInstance<ServiceProvider>()
              .forEach { it.provideServices(registry) }
    }

    /**
     * 从 registry 中注销本 ViewModel 的所有服务
     */
    fun detachFromRegistry(registry: MutableServiceRegistry) {
        useCases.filterIsInstance<ServiceProvider>()
              .forEach { registry.unregister(it) }
    }
}
```

### 4. Compose 集成（`:scaffold:android`）

```kotlin
// ServiceRegistryCompositionLocal.kt
package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LocalLifecycleOwner
import zhaoyun.example.composedemo.scaffold.core.mvi.MutableServiceRegistry

/**
 * CompositionLocal —— 向下传递当前 Screen 的 ServiceRegistry
 */
val LocalServiceRegistry = compositionLocalOf<MutableServiceRegistry?> { null }

/**
 * Screen 级别的 Registry Provider —— 推荐所有 Screen 根 Composable 包裹此组件
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

```kotlin
// ScreenViewModel.kt
package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder

/**
 * screenViewModel() —— koinViewModel() 的包装，自动注册到当前 Screen 的 ServiceRegistry
 *
 * 所有位于 Screen 作用域内的 ViewModel 都应通过此 API 获取，而非直接使用 koinViewModel()
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

### 5. ServiceRegistry 实现（`:scaffold:android`，含 Koin fallback）

```kotlin
// MutableServiceRegistryImpl.kt
package zhaoyun.example.composedemo.scaffold.android

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.error.NoBeanDefFoundException
import zhaoyun.example.composedemo.scaffold.core.mvi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.mvi.ServiceRegistry

/**
 * MutableServiceRegistry 的默认实现
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

## 使用示例

### 场景：MessageUseCase 需要调用 Analytics 服务

**Step 1：定义服务接口（`:domain` 层）**

```kotlin
package zhaoyun.example.composedemo.story.message.domain

interface MessageAnalytics {
    fun trackMessageClicked(dialogueId: String)
}
```

**Step 2：StoryCardUseCase 实现服务（父级 Screen）**

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
        // 实际埋点逻辑
    }

    override suspend fun onEvent(event: StoryCardEvent) {
        // ...
    }
}
```

**Step 3：MessageUseCase 发现并使用服务（子级）**

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

**Step 4：Screen Composable 中使用 `ServiceRegistryProvider` + `screenViewModel()`**

```kotlin
package zhaoyun.example.composedemo.story.presentation

import androidx.compose.runtime.Composable
import org.koin.core.parameter.parametersOf
import zhaoyun.example.composedemo.scaffold.android.ServiceRegistryProvider
import zhaoyun.example.composedemo.scaffold.android.screenViewModel

@Composable
fun StoryCardPage(card: StoryCard) {
    // ServiceRegistryProvider 创建 Screen 级别的 registry
    ServiceRegistryProvider {

        val storyVm = screenViewModel<StoryCardViewModel>()

        val messageVm = screenViewModel<MessageViewModel> {
            parametersOf(storyVm.messageStateHolder)
        }

        val infoBarVm = screenViewModel<InfoBarViewModel> {
            parametersOf(storyVm.infoBarStateHolder, card.cardId)
        }

        // Composable 也可以直接读取 registry（如果需要）
        val registry = LocalServiceRegistry.current

        Box(modifier = Modifier.fillMaxSize()) {
            MessageArea(viewModel = messageVm)
            InfoBarArea(viewModel = infoBarVm)
        }
    }
}
```

### 作用域链示例：子 Screen 继承父 Screen 服务

```kotlin
@Composable
fun ParentScreen() {
    ServiceRegistryProvider {
        val parentVm = screenViewModel<ParentViewModel>()
        // ParentViewModel 的 UseCase 注册了 AuthProvider 服务

        // 子 Screen 包裹在 ParentScreen 内部
        ChildScreen()
    }
}

@Composable
fun ChildScreen() {
    // 子 Screen 也包裹 ServiceRegistryProvider，不传 parent 则自动从 CompositionLocal 继承
    ServiceRegistryProvider {
        val childVm = screenViewModel<ChildViewModel>()

        // ChildViewModel 的 UseCase 中：
        // findService<AuthProvider>() → 查 ChildScreen 的 registry（本地没有）
        //                               → fallback 到 ParentScreen 的 registry（找到！）
    }
}
```

## 查找顺序总结

```
MessageUseCase.findService<MessageAnalytics>()
    ↓
MutableServiceRegistryImpl.find(MessageAnalytics::class)
    ├── 1. local[MessageAnalytics] ?          ← 同 Screen 内 UseCase 注册
    ├── 2. parent.find(MessageAnalytics) ?    ← 父 Screen 的 registry（作用域链）
    └── 3. koin.get(MessageAnalytics) ?       ← Koin 全局容器（兜底）
```

## 与现有架构的关系

| 机制 | 职责 | 对比 |
|------|------|------|
| **StateHolder（状态流）** | UseCase 之间共享状态 | 数据通信，持续存在 |
| **Effect（副作用流）** | 一次性事件（Toast、导航） | 事件通知，不保证送达 |
| **ServiceRegistry（服务发现）** | UseCase 之间调用行为接口 | 行为调用，类型安全 |
| **Koin（全局 DI）** | 跨模块依赖注入 | 模块级组装，生命周期长 |

三者并行不冲突：
- 状态变更 → 用 `StateHolder`
- 一次性通知 → 用 `Effect`
- 调用另一个 UseCase 的方法 → 用 `findService<T>()`

## 边界情况与降级策略

### 1. findService 找不到时

- **默认行为：** 抛异常，提示"是否忘记实现 ServiceProvider"
- **安全查找：** 使用 `findServiceOrNull<T>()` 返回 null，由业务自行处理

### 2. 多个 UseCase 注册同一接口

- **后注册者覆盖先注册者**（`MutableServiceRegistryImpl` 使用 `Map`）
- 设计约定：同一个 Screen 内，同一接口只应由一个 UseCase 提供

### 3. 已有 ViewModel 未使用 screenViewModel()

- **兼容：** 现有代码完全不受影响。`BaseViewModel.attachToRegistry()` 是 public 的，可以手动调用
- **迁移：** 逐步将 `koinViewModel()` 替换为 `screenViewModel()`

### 4. 单元测试

```kotlin
class MessageUseCaseTest {
    private val useCase = MessageUseCase()
    private val mockAnalytics = mockk<MessageAnalytics>()

    @Test
    fun `点击对话时触发埋点`() = runTest {
        // 测试时手动组装 registry，无需 Compose 环境
        val registry = MutableServiceRegistryImpl()
        registry.register<MessageAnalytics>(mockAnalytics)
        useCase.attachServiceRegistry(registry)

        useCase.onEvent(MessageEvent.OnDialogueClicked("d_1"))

        verify { mockAnalytics.trackMessageClicked("d_1") }
    }
}
```

## 验证标准

- [ ] `:scaffold:core` 新增 `ServiceRegistry.kt`，纯 Kotlin，零 Android 依赖
- [ ] `:scaffold:android` 新增 `ServiceRegistryCompositionLocal.kt`、`ScreenViewModel.kt`、`MutableServiceRegistryImpl.kt`
- [ ] `BaseUseCase` 新增 `attachServiceRegistry` / `findService<T>()` / `findServiceOrNull<T>()`
- [ ] `BaseViewModel` 新增 `attachToRegistry()` / `detachFromRegistry()`
- [ ] `StoryCardPage` 使用 `ServiceRegistryProvider` + `screenViewModel()`，编译通过
- [ ] `MessageUseCase` 通过 `findService<MessageAnalytics>()` 调用父级服务，测试通过
- [ ] `./gradlew test` 全量通过
- [ ] 现有未改造的 ViewModel/UseCase 不受影响（向后兼容）
