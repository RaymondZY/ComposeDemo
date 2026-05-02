# ServiceRegistry Koin 中转重构设计

> 目标：去除 `attachParent` 链式挂接，借助 Koin Scope 实现 Screen 级别的 ServiceRegistry 共享，使 ViewModel、CombineUseCase、BaseUseCase 在 `init{}` 中即可完成自注册。

---

## 1. 背景与目标

### 1.1 当前问题

- `attachParent` 在 4 个层级（`screenViewModel()` → `BaseViewModel` → `CombineUseCase` → `BaseUseCase`）手动挂接，形成链式查找结构，代码冗余且容易遗漏。
- UseCase 位于 Domain 层，无法直接访问 Compose 的 `CompositionLocal`，导致 `ServiceRegistryProvider` 无法直接下沉到 UseCase。
- 每个节点（VM / CombineUseCase / UseCase）各自持有 `MutableServiceRegistryImpl`，层级复杂，清理逻辑分散。

### 1.2 设计目标

1. **彻底去除 `attachParent`**：不再手动挂接 parent registry。
2. **构造方法零侵入**：`BaseViewModel`、`CombineUseCase`、`BaseUseCase` 的构造函数签名保持不变。
3. **自注册**：所有节点在 `init{}` 中通过公共 API 获取当前 Screen 的 `ServiceRegistry`，并自动注册自己实现的 `MviService`。
4. **Koin 做中转**：Compose 层创建 Koin Scope，将 `ServiceRegistry` 注册为 Scope bean；下层通过 Koin Scope 获取。
5. **生命周期安全**：VM 销毁时自动反注册，避免内存泄漏。

---

## 2. 术语

| 术语 | 含义 |
|------|------|
| `MviScreenScope` | 每个 Screen 对应的 Koin `Scope`，生命周期与 Screen 绑定 |
| `ScreenScopeStack` | 全局栈结构，保存当前激活的 Koin Scope，解决非 Koin 创建的 UseCase 如何定位当前 Scope |
| `ServiceRegistry` | Screen 级别的服务注册表，该 Screen 下所有节点共享同一个实例 |
| `autoRegister` | 自动扫描并注册当前实例实现的所有 `MviService` 子接口 |
| `autoUnregister` | 将指定实例从 `ServiceRegistry` 中移除 |

---

## 3. 架构总览

### 3.1 变化前后对比

**当前架构（含 attachParent）：**

```
StoryCardPage (Compose)
├── ServiceRegistryProvider(registry=StoryCardVM.registry)
│   └── screenViewModel()
│       ├── attachParent(parentRegistry)              ← 手动挂接
│       └── MessageViewModel
│           ├── serviceRegistry (MutableServiceRegistryImpl)
│           └── CombineUseCase
│               ├── attachParent(VM.registry)           ← 手动挂接
│               ├── serviceRegistry (MutableServiceRegistryImpl)
│               ├── MessageUseCase
│               │   ├── attachParent(CombineUseCase.registry)
│               │   └── serviceRegistry
│               └── StoryCardUseCase (auto-register)
```

**目标架构（去除 attachParent，Koin Scope 中转）：**

```
StoryCardPage (Compose)
└── MviScreen                                          ← 创建 Koin Scope
    ├── Scope 内 bean: ServiceRegistry = MutableServiceRegistryImpl()
    ├── ScreenScopeStack.push(scope)
    │
    ├── screenViewModel(scope=currentScope)
    │   └── MessageViewModel (init{} 中 autoRegister)
    │       └── CombineUseCase (init{} 中 autoRegister)
    │           ├── MessageUseCase (init{} 中 autoRegister)
    │           └── StoryCardUseCase (init{} 中 autoRegister)
    │
    └── Screen 退出: ScreenScopeStack.pop(), scope.close()
```

### 3.2 核心变化点

| 项目 | 当前 | 目标 |
|------|------|------|
| Registry 实例 | 每个 VM / CombineUseCase / UseCase 各一个 | **每个 Screen 一个**，所有节点共享 |
| 作用域链 | `attachParent` 链式挂接 | **Koin Scope 统一管理**，无 parent 链 |
| 服务注册 | `CombineUseCase` 构造函数内 + 运行时 `attachParent` | **各节点 `init{}` 自行 `autoRegister`** |
| 服务查找链 | 本地 → parent → ... → Screen registry | **单 Screen registry**，直接查找 |
| VM 创建 | `koinViewModel()` + 手动 `attachParent` | `koinViewModel(scope=screenScope)` |
| 清理时机 | `BaseViewModel.onCleared()` 各自 `clear()` | **VM `onCleared()` 逐个 `autoUnregister`** |

---

## 4. 核心设计

### 4.1 Koin Scope 管理

每个 Screen 创建一个独立的 Koin `Scope`，`ServiceRegistry` 注册为该 Scope 的 bean。

```kotlin
@Composable
fun MviScreen(
    viewModel: BaseViewModel<*, *, *>,
    content: @Composable () -> Unit
) {
    val koin = getKoin()
    val screenRegistry = remember { MutableServiceRegistryImpl() }
    val scope = remember(viewModel) {
        val scopeId = "MviScreen_${viewModel.hashCode()}_${System.currentTimeMillis()}"
        koin.createScope(scopeId, qualifier = named("MviScreenScope"))
    }

    DisposableEffect(scope) {
        scope.declare(screenRegistry, allowOverride = true)
        onDispose {
            screenRegistry.clear()
            scope.close()
        }
    }

    ServiceRegistryProvider(registry = screenRegistry) {
        CompositionLocalProvider(LocalKoinScope provides scope) {
            content()
        }
    }
}
```

**设计要点：**
- Scope ID 使用 `viewModel.hashCode() + 时间戳` 确保唯一性。
- `scope.declare()` 将 Screen 的 `MutableServiceRegistryImpl` 注册为该 Scope 的 bean。
- Screen 退出 Compose 时，`DisposableEffect` 的 `onDispose` 清理 registry 并关闭 Scope。
- `LocalServiceRegistry` 继续保留，向后兼容现有代码。

### 4.2 ScreenScopeStack

全局栈结构，维护当前激活的 Koin Scope。因为 `BaseUseCase` 不是由 Koin 创建的，无法直接注入 Scope，需要通过栈来传递上下文。

```kotlin
object ScreenScopeStack {
    private val stack = ArrayDeque<Scope>()

    val current: Scope? get() = stack.lastOrNull()

    fun push(scope: Scope) {
        stack.addLast(scope)
    }

    fun pop() {
        stack.removeLastOrNull()
    }

    fun requireCurrent(): Scope {
        return current ?: error(
            "No active Koin Scope found. " +
            "Make sure this is created within a Screen Koin Scope."
        )
    }
}
```

### 4.3 ServiceRegistry 获取与注册

#### 4.3.1 requireServiceRegistry()

公共方法，从当前栈顶 Scope 获取 `MutableServiceRegistry`，非空断言。

```kotlin
fun requireServiceRegistry(): MutableServiceRegistry {
    return ScreenScopeStack.requireCurrent().get()
}
```

#### 4.3.2 autoRegister / autoUnregister

```kotlin
fun Any.autoRegister(registry: MutableServiceRegistry) {
    val clazz = this::class.java
    val interfaces = clazz.allSuperInterfaces()
    for (interfaceType in interfaces) {
        if (interfaceType == MviService::class.java || interfaceType == TaggedMviService::class.java) {
            continue
        }
        if (MviService::class.java.isAssignableFrom(interfaceType)) {
            if (TaggedMviService::class.java.isAssignableFrom(interfaceType)) {
                val tag = (this as TaggedMviService).serviceTag
                registry.register(interfaceType, this, tag)
            } else {
                registry.register(interfaceType, this)
            }
        }
    }
}

fun Any.autoUnregister(registry: MutableServiceRegistry) {
    registry.unregister(this)
}
```

### 4.4 BaseUseCase 改造

```kotlin
abstract class BaseUseCase<S, E, F>(
    override val stateHolder: StateHolder<S>
) : MviFacade<S, E, F> {

    init {
        autoRegister(requireServiceRegistry())
    }
}
```

### 4.5 CombineUseCase 改造

```kotlin
class CombineUseCase<S, E, F>(
    override val stateHolder: StateHolder<S>,
    vararg useCaseCreators: UseCaseFactory<S, E, F>,
) : MviFacade<S, E, F> {

    private val childUseCases = useCaseCreators.map { it(this.stateHolder) }

    init {
        autoRegister(requireServiceRegistry())
    }

    internal fun allUseCases(): List<BaseUseCase<*, *, *>> = childUseCases
}
```

### 4.6 BaseViewModel 改造

```kotlin
open class BaseViewModel<S, E, F>(
    override val stateHolder: StateHolder<S>,
    vararg useCaseCreators: UseCaseFactory<S, E, F>,
) : ViewModel(), MviFacade<S, E, F> {

    private val combineUseCase = CombineUseCase(stateHolder, *useCaseCreators)

    // init{} 期间从 Stack 获取，之后不再依赖 Stack
    private val screenRegistry: MutableServiceRegistry = requireServiceRegistry()

    init {
        autoRegister(screenRegistry)
    }

    override fun onCleared() {
        // 反注册整棵树：子 UseCase → CombineUseCase → VM 自己
        combineUseCase.allUseCases().forEach { screenRegistry.autoUnregister(it) }
        screenRegistry.autoUnregister(combineUseCase)
        screenRegistry.autoUnregister(this)
        super.onCleared()
    }
}
```

### 4.7 screenViewModel() 改造

```kotlin
@Composable
inline fun <reified D, reified VM : BaseViewModel<*, *, *>> screenViewModel(
    keyData: D,
    noinline parameters: (() -> ParametersHolder)? = null
): VM {
    val scope = ScreenScopeStack.current
        ?: error("screenViewModel() must be called inside a Screen Koin Scope")

    ScreenScopeStack.push(scope)
    try {
        val key = VM::class.simpleName + " " + D::class.simpleName + " " + keyData.hashCode()
        val params = parameters ?: { parametersOf(keyData) }
        return koinViewModel<VM>(key = key, scope = scope, parameters = params)
    } finally {
        ScreenScopeStack.pop()
    }
}
```

**关键执行时序：**

```
screenViewModel(card) 开始
  ├─ ScreenScopeStack.push(storyScope)
  ├─ koinViewModel(scope = storyScope) 开始创建
  │   ├─ BaseViewModel 属性初始化
  │   │   └─ CombineUseCase(stateHolder, *factories)
  │   │       ├─ 属性初始化：map { factory(stateHolder) }
  │   │       │   └─ MessageUseCase 创建
  │   │       │       └─ init{} 执行
  │   │       │           └─ autoRegister(requireServiceRegistry()) ✅
  │   │       └─ CombineUseCase init{} 执行
  │   │           └─ autoRegister(requireServiceRegistry()) ✅
  │   └─ BaseViewModel init{} 执行
  │       └─ autoRegister(requireServiceRegistry()) ✅
  └─ koinViewModel() 返回
  └─ ScreenScopeStack.pop()
```

`push` → `koinViewModel()` 创建（同步完成所有 `init{}`） → `pop` 是**同一线程同步流程**，所有 `init{}` 执行期间 `ScreenScopeStack.current` 始终有效。

---

## 5. 生命周期与反注册

### 5.1 注册时机

所有节点在 `init{}` 中同步完成自注册。**不允许**在异步协程中注册，否则 `ScreenScopeStack` 可能已弹出。

### 5.2 反注册时机

- **ViewModel**：`onCleared()` 中反注册以它为根的整棵树（子 UseCase → CombineUseCase → VM 自己）。
- **CombineUseCase / BaseUseCase**：不单独处理生命周期，由父 ViewModel 统一反注册。
- **Screen 级别兜底**：`MviScreen` 的 `DisposableEffect.onDispose` 中调用 `registry.clear()` + `scope.close()`，作为最终兜底。

### 5.3 为什么缓存 screenRegistry

`screenViewModel()` 在 `finally` 中弹出 Stack，而 `ViewModel.onCleared()` 可能在很久以后（VM 真正销毁时）才执行。因此 `BaseViewModel` 在 `init{}` 中将 `requireServiceRegistry()` 的结果缓存到 `private val screenRegistry`，后续反注册不再依赖 Stack。

---

## 6. 兼容性 & 迁移策略

### 6.1 三阶段迁移

**Phase 1：新增机制（不破坏现有代码）**
- 新增 `ScreenScopeStack`、`requireServiceRegistry()`、`autoRegister()`、`autoUnregister()`。
- 新增 `MviScreen` 的 Koin Scope 创建逻辑。
- `screenViewModel()` 支持传入 `scope`。
- `BaseViewModel`、`CombineUseCase`、`BaseUseCase` 同时保留新旧两套逻辑。

**Phase 2：双轨运行**
- `attachParent`、`detachParent` 标记 `@Deprecated`，提示迁移。
- `CombineUseCase` 中的 `registerAutoServices` 保留但标记 deprecated。
- 业务代码逐步迁移到新的自注册模式。

**Phase 3：去除旧逻辑**
- 删除 `attachParent`、`detachParent` 接口及实现。
- 删除 `screenViewModel()` 中的 `viewModel.attachParent(registry)` 调用。
- 删除 `CombineUseCase` 中的旧 `registerAutoServices` 调用。

### 6.2 对现有代码的影响

| 代码位置 | 影响 |
|----------|------|
| 业务 Screen（如 `StoryCardPage`） | 无感知，`MviScreen` 内部改造 |
| `BaseViewModel` 子类 | 如果子类覆写了 `onCleared()` 并调用了 `serviceRegistry.clear()`，需确认是否和 Scope 关闭重复 |
| `BaseUseCase` 子类 | 无感知，自动在 `init{}` 中注册 |
| 手动调用 `attachParent` 的代码 | Deprecated，迁移期后删除 |

---

## 7. 测试策略

### 7.1 单元测试

- `ScreenScopeStack`：验证 push/pop 栈行为，空栈时 `requireCurrent()` 抛异常。
- `autoRegister` / `autoUnregister`：验证正确扫描 `MviService` 子接口，带 tag 和无 tag 场景。
- `requireServiceRegistry()`：验证无 Scope 时抛异常。

### 7.2 集成测试

- `BaseViewModel` + `CombineUseCase` + `BaseUseCase`：验证 `init{}` 中所有节点正确注册到同一个 Screen registry。
- `BaseViewModel.onCleared()`：验证反注册后，`findService()` 返回 null。
- `screenViewModel()`：验证 VM 在 Koin Scope 内创建，`init{}` 执行期间 Stack 有效。

### 7.3 生命周期测试

- Screen 退出后，Koin Scope 关闭，`ServiceRegistry.clear()` 被调用，无内存泄漏。
- 子 VM（如 `MessageViewModel`）单独销毁时，只移除它注册的服务，不影响 Screen 级别其他服务。

---

## 8. 风险与回滚

| 风险 | 缓解措施 |
|------|----------|
| `ScreenScopeStack` 全局状态引入 | 栈操作限定在 `screenViewModel()` 的同步创建路径，无并发问题；Android Compose 主线程单线程执行 |
| `init{}` 中依赖 Stack，若有人异步创建 UseCase | 编码规范：所有 `autoRegister` 必须在构造同步路径中完成；如违反，`requireServiceRegistry()` 会抛异常，fail-fast |
| 旧 `attachParent` 和新 `autoRegister` 同时运行导致重复注册 | Phase 1 中，`MutableServiceRegistryImpl.register` 已具备 fail-fast（重复 key 抛异常），问题会及时发现 |
| 迁移期间新旧代码混用导致行为不一致 | 三阶段迁移，每阶段有明确的验收标准和 Code Review 检查清单 |

---

## 9. 附录：API 汇总

```kotlin
// scaffold/core - SPI
object ScreenScopeStack {
    val current: Scope?
    fun push(scope: Scope)
    fun pop()
    fun requireCurrent(): Scope
}

fun requireServiceRegistry(): MutableServiceRegistry

fun Any.autoRegister(registry: MutableServiceRegistry)
fun Any.autoUnregister(registry: MutableServiceRegistry)

// scaffold/android - Compose
@Composable
fun MviScreen(viewModel: BaseViewModel<*, *, *>, content: @Composable () -> Unit)

@Composable
inline fun <reified D, reified VM : BaseViewModel<*, *, *>> screenViewModel(...): VM

// scaffold/core - UseCase / VM
abstract class BaseUseCase<S, E, F>
class CombineUseCase<S, E, F>
open class BaseViewModel<S, E, F>
```
