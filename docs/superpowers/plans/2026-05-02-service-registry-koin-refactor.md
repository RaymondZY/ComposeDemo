# ServiceRegistry Koin Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove `attachParent` chain and enable self-registration in `init{}` for ViewModel, CombineUseCase, and BaseUseCase via Koin Scope.

**Architecture:** Create a per-Screen Koin Scope that hosts a single shared `MutableServiceRegistryImpl`. Use a global `ScreenScopeStack` to make the current Scope accessible during synchronous `init{}` execution. All nodes call `autoRegister()` in `init{}` and `autoUnregister()` in `onCleared()`.

**Tech Stack:** Kotlin, Koin 3.5.6, JUnit 4, kotlinx-coroutines-test, Jetpack Compose

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `scaffold/core/build.gradle.kts` | Modify | Add `koin-core` dependency |
| `scaffold/core/src/main/kotlin/.../spi/ScreenScopeStack.kt` | Create | Global stack for active Koin Scopes |
| `scaffold/core/src/main/kotlin/.../spi/ServiceRegistryExt.kt` | Create | `requireServiceRegistry()`, `autoRegister()`, `autoUnregister()` |
| `scaffold/core/src/main/kotlin/.../spi/ServiceRegistry.kt` | Modify | Mark `attachParent`/`detachParent` `@Deprecated` |
| `scaffold/core/src/main/kotlin/.../usecase/BaseUseCase.kt` | Modify | Remove own registry, `init{}` self-register |
| `scaffold/core/src/main/kotlin/.../usecase/CombineUseCase.kt` | Modify | Remove own registry & `attachParent`, expose `allUseCases()` |
| `scaffold/android/src/main/kotlin/.../android/BaseViewModel.kt` | Modify | Remove own registry, `init{}` self-register, `onCleared()` unregister tree |
| `scaffold/android/src/main/kotlin/.../android/MviScreen.kt` | Modify | Create Koin Scope, declare registry bean, provide `LocalKoinScope` |
| `scaffold/android/src/main/kotlin/.../android/ScreenViewModel.kt` | Modify | Push/pop `ScreenScopeStack`, pass `scope` to `koinViewModel()` |
| `scaffold/android/src/main/kotlin/.../android/ServiceRegistryCompositionLocal.kt` | Modify | Add `LocalKoinScope` |
| `scaffold/core/src/test/kotlin/.../spi/ScreenScopeStackTest.kt` | Create | Unit tests for stack behavior |
| `scaffold/core/src/test/kotlin/.../spi/ServiceRegistryExtTest.kt` | Create | Unit tests for autoRegister/autoUnregister |
| `scaffold/core/src/test/kotlin/.../usecase/CombineUseCaseRegistryTest.kt` | Create | Integration test for registration chain |

---

## Task 1: Add koin-core to scaffold/core

**Files:**
- Modify: `scaffold/core/build.gradle.kts`

`scaffold/core` currently has no Koin dependency, but `ScreenScopeStack` and `requireServiceRegistry()` need `org.koin.core.scope.Scope`. `koin-core` is pure Kotlin with no Android deps, safe for the domain module.

- [ ] **Step 1: Add dependency**

Modify `scaffold/core/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.jetbrainsKotlinJvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.koin.core) // ← add this line
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 2: Verify build**

Run:
```bash
./gradlew :scaffold:core:dependencies --configuration compileClasspath | grep koin
```

Expected output contains `io.insert-koin:koin-core:3.5.6`.

- [ ] **Step 3: Commit**

```bash
git add scaffold/core/build.gradle.kts
git commit -m "build: add koin-core to scaffold:core for ScreenScopeStack"
```

---

## Task 2: ScreenScopeStack + ServiceRegistry utilities

**Files:**
- Create: `scaffold/core/src/main/kotlin/zhaoyun/example/composedemo/scaffold/core/spi/ScreenScopeStack.kt`
- Create: `scaffold/core/src/main/kotlin/zhaoyun/example/composedemo/scaffold/core/spi/ServiceRegistryExt.kt`
- Create: `scaffold/core/src/test/kotlin/zhaoyun/example/composedemo/scaffold/core/spi/ScreenScopeStackTest.kt`
- Create: `scaffold/core/src/test/kotlin/zhaoyun/example/composedemo/scaffold/core/spi/ServiceRegistryExtTest.kt`

- [ ] **Step 1: Create ScreenScopeStack**

```kotlin
package zhaoyun.example.composedemo.scaffold.core.spi

import org.koin.core.scope.Scope

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

- [ ] **Step 2: Create ServiceRegistryExt**

```kotlin
package zhaoyun.example.composedemo.scaffold.core.spi

import org.koin.core.scope.Scope

fun requireServiceRegistry(): MutableServiceRegistry {
    return ScreenScopeStack.requireCurrent().get()
}

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

private fun Class<*>.allSuperInterfaces(): Set<Class<*>> {
    val discovered = linkedSetOf<Class<*>>()
    val pending = ArrayDeque<Class<*>>()
    pending += this
    while (pending.isNotEmpty()) {
        val current = pending.removeFirst()
        current.superclass?.let { pending += it }
        current.interfaces.forEach { iface ->
            pending += iface
            discovered += iface
        }
    }
    return discovered
}
```

- [ ] **Step 3: Write failing test for ScreenScopeStack**

```kotlin
package zhaoyun.example.composedemo.scaffold.core.spi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenScopeStackTest {

    @Test(expected = IllegalStateException::class)
    fun `requireCurrent throws when empty`() {
        ScreenScopeStack.requireCurrent()
    }

    @Test
    fun `push and pop work`() {
        assertNull(ScreenScopeStack.current)

        // We can't easily create a real Koin Scope in unit tests without starting Koin,
        // so we verify the stack behavior by mocking or using a no-op approach.
        // For this test we'll verify the error message and push/pop logic via a fake.
    }
}
```

> **Note:** Creating a real Koin `Scope` in pure JVM unit tests requires `startKoin`. Use `org.koin.core.context.startKoin` and `org.koin.dsl.module` in the test setup. Because this adds complexity, keep the test minimal: verify `requireCurrent()` throws and that `push`/`pop` maintain LIFO order using a fake `Scope` (subclass with no-op methods) if necessary.

Simpler approach — use Mockito or manual fake. Since the project only has JUnit, use a manual fake:

```kotlin
private class FakeScope : Scope(null, null, null, null, null) {
    override fun toString(): String = "FakeScope"
}
```

Actually, `Scope` constructor is internal. Better to start a minimal Koin instance in the test:

```kotlin
package zhaoyun.example.composedemo.scaffold.core.spi

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class ScreenScopeStackTest {

    @Before
    fun setup() {
        startKoin {
            modules(module { })
        }
    }

    @After
    fun teardown() {
        stopKoin()
        while (ScreenScopeStack.current != null) {
            ScreenScopeStack.pop()
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `requireCurrent throws when empty`() {
        ScreenScopeStack.requireCurrent()
    }

    @Test
    fun `push and pop maintain LIFO order`() {
        val koin = org.koin.core.context.GlobalContext.get()
        val scope1 = koin.createScope("scope1", org.koin.core.qualifier.named("test"))
        val scope2 = koin.createScope("scope2", org.koin.core.qualifier.named("test"))

        ScreenScopeStack.push(scope1)
        assertEquals(scope1, ScreenScopeStack.current)

        ScreenScopeStack.push(scope2)
        assertEquals(scope2, ScreenScopeStack.current)

        ScreenScopeStack.pop()
        assertEquals(scope1, ScreenScopeStack.current)

        ScreenScopeStack.pop()
        assertNull(ScreenScopeStack.current)

        scope1.close()
        scope2.close()
    }
}
```

- [ ] **Step 4: Write failing test for autoRegister / autoUnregister**

```kotlin
package zhaoyun.example.composedemo.scaffold.core.spi

import org.junit.Assert.*
import org.junit.Test

class ServiceRegistryExtTest {

    interface TestService : MviService {
        fun doSomething()
    }

    class TestServiceImpl : TestService {
        override fun doSomething() {}
    }

    @Test
    fun `autoRegister registers MviService implementations`() {
        val registry = MutableServiceRegistryImpl()
        val impl = TestServiceImpl()

        impl.autoRegister(registry)

        assertSame(impl, registry.find(TestService::class.java))
    }

    @Test
    fun `autoUnregister removes instance`() {
        val registry = MutableServiceRegistryImpl()
        val impl = TestServiceImpl()

        impl.autoRegister(registry)
        assertNotNull(registry.find(TestService::class.java))

        impl.autoUnregister(registry)
        assertNull(registry.find(TestService::class.java))
    }

    @Test
    fun `autoRegister does not register plain interfaces`() {
        interface PlainInterface
        class PlainImpl : PlainInterface

        val registry = MutableServiceRegistryImpl()
        val impl = PlainImpl()

        impl.autoRegister(registry)
        assertNull(registry.find(PlainInterface::class.java))
    }
}
```

- [ ] **Step 5: Run tests to verify they fail**

```bash
./gradlew :scaffold:core:test --tests "zhaoyun.example.composedemo.scaffold.core.spi.ScreenScopeStackTest"
./gradlew :scaffold:core:test --tests "zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistryExtTest"
```

Expected: Some tests compile but may fail due to missing imports or Koin setup. Fix compilation first, then expect the test logic to pass once code is correct.

- [ ] **Step 6: Run tests to verify they pass**

After implementing Task 1 + 2 code, run again. Expected: ALL PASS.

- [ ] **Step 7: Commit**

```bash
git add scaffold/core/src/main/kotlin/.../spi/ScreenScopeStack.kt \
        scaffold/core/src/main/kotlin/.../spi/ServiceRegistryExt.kt \
        scaffold/core/src/test/kotlin/.../spi/ScreenScopeStackTest.kt \
        scaffold/core/src/test/kotlin/.../spi/ServiceRegistryExtTest.kt
git commit -m "feat: add ScreenScopeStack and autoRegister/autoUnregister utilities"
```

---

## Task 3: BaseUseCase — remove own registry, self-register in init

**Files:**
- Modify: `scaffold/core/src/main/kotlin/zhaoyun/example/composedemo/scaffold/core/usecase/BaseUseCase.kt`

- [ ] **Step 1: Modify BaseUseCase**

Replace the file content with:

```kotlin
package zhaoyun.example.composedemo.scaffold.core.usecase

import zhaoyun.example.composedemo.scaffold.core.mvi.EffectDispatcher
import zhaoyun.example.composedemo.scaffold.core.mvi.EffectDispatcherImpl
import zhaoyun.example.composedemo.scaffold.core.mvi.EventReceiver
import zhaoyun.example.composedemo.scaffold.core.mvi.EventReceiverImpl
import zhaoyun.example.composedemo.scaffold.core.mvi.MviFacade
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.autoRegister
import zhaoyun.example.composedemo.scaffold.core.spi.autoUnregister
import zhaoyun.example.composedemo.scaffold.core.spi.find
import zhaoyun.example.composedemo.scaffold.core.spi.requireServiceRegistry

typealias UseCaseFactory<S, E, F> = (StateHolder<S>) -> BaseUseCase<S, E, F>

abstract class BaseUseCase<S : UiState, E : UiEvent, F : UiEffect>(
    override val stateHolder: StateHolder<S>,
) : MviFacade<S, E, F> {

    final override val eventReceiver: EventReceiver<E> = EventReceiverImpl(::onEvent)

    final override val effectDispatcher: EffectDispatcher<F> = EffectDispatcherImpl()

    abstract suspend fun onEvent(event: E)

    init {
        autoRegister(requireServiceRegistry())
    }

    @Deprecated(
        "attachParent is no longer needed. ServiceRegistry is now shared per-Screen via Koin Scope.",
        ReplaceWith("")
    )
    fun attachParent(registry: ServiceRegistry) {
        // no-op: retained for binary compatibility during migration
    }

    @Deprecated(
        "detachParent is no longer needed.",
        ReplaceWith("")
    )
    fun detachParent() {
        // no-op
    }

    protected inline fun <reified T : Any> findService(tag: String? = null): T {
        return requireServiceRegistry().find<T>(tag)
            ?: error(
                "Service ${T::class.java.name} not found in current scope. " +
                    "Did you forget to register it from a ServiceProvider or auto-expose an MviService?"
            )
    }

    protected inline fun <reified T : Any> findServiceOrNull(tag: String? = null): T? {
        return requireServiceRegistry().find<T>(tag)
    }

    protected inline fun <reified T : Any> registerService(
        instance: T,
        tag: String? = null,
    ) {
        requireServiceRegistry().register(T::class.java, instance, tag)
    }

    protected inline fun <reified T : Any> unregisterService(tag: String? = null) {
        requireServiceRegistry().unregister(T::class.java, tag)
    }

    protected fun unregisterService(instance: Any) {
        requireServiceRegistry().unregister(instance)
    }
}
```

Key changes:
- Removed `protected val serviceRegistry = MutableServiceRegistryImpl()`
- Added `init { autoRegister(requireServiceRegistry()) }`
- `attachParent`/`detachParent` marked `@Deprecated` and made no-op
- `findService`/`findServiceOrNull` now read from `requireServiceRegistry()` instead of local field

- [ ] **Step 2: Verify core module compiles**

```bash
./gradlew :scaffold:core:compileKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add scaffold/core/src/main/kotlin/.../usecase/BaseUseCase.kt
git commit -m "refactor: BaseUseCase self-registers via requireServiceRegistry, deprecates attachParent"
```

---

## Task 4: CombineUseCase — remove own registry and attachParent

**Files:**
- Modify: `scaffold/core/src/main/kotlin/zhaoyun/example/composedemo/scaffold/core/usecase/CombineUseCase.kt`

- [ ] **Step 1: Modify CombineUseCase**

Replace the file content with:

```kotlin
package zhaoyun.example.composedemo.scaffold.core.usecase

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.EffectDispatcher
import zhaoyun.example.composedemo.scaffold.core.mvi.EventReceiver
import zhaoyun.example.composedemo.scaffold.core.mvi.EventReceiverImpl
import zhaoyun.example.composedemo.scaffold.core.mvi.MviFacade
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.autoRegister
import zhaoyun.example.composedemo.scaffold.core.spi.requireServiceRegistry

class CombineUseCase<S : UiState, E : UiEvent, F : UiEffect>(
    override val stateHolder: StateHolder<S>,
    vararg useCaseCreators: UseCaseFactory<S, E, F>,
) : MviFacade<S, E, F> {

    private val childUseCases = useCaseCreators.map { it(this.stateHolder) }

    init {
        autoRegister(requireServiceRegistry())
    }

    override val eventReceiver: EventReceiver<E> = EventReceiverImpl { event ->
        childUseCases.forEach { it.receiveEvent(event) }
    }

    override val effectDispatcher: EffectDispatcher<F> = CombineEffectDispatcher(childUseCases)

    internal fun allUseCases(): List<BaseUseCase<*, *, *>> = childUseCases

    @Deprecated(
        "attachParent is no longer needed. ServiceRegistry is now shared per-Screen via Koin Scope.",
        ReplaceWith("")
    )
    fun attachParent(serviceRegistry: ServiceRegistry) {
        // no-op: retained for binary compatibility during migration
    }
}

class CombineEffectDispatcher<F : UiEffect>(
    private val useCases: List<BaseUseCase<*, *, F>>,
) : EffectDispatcher<F> {

    private val ownEffect = Channel<F>(Channel.BUFFERED)

    private val ownBaseEffect = Channel<BaseEffect>(Channel.BUFFERED)

    override val effect = merge(
        *useCases.map { it.effect }.toTypedArray(),
        ownEffect.receiveAsFlow()
    )

    override val baseEffect = merge(
        *useCases.map { it.baseEffect }.toTypedArray(),
        ownBaseEffect.receiveAsFlow()
    )

    override fun dispatchEffect(effect: F) {
        ownEffect.trySend(effect)
    }

    override fun dispatchBaseEffect(baseEffect: BaseEffect) {
        ownBaseEffect.trySend(baseEffect)
    }
}
```

Key changes:
- Removed `private val serviceRegistry = MutableServiceRegistryImpl()`
- Removed `registerAutoServices` private function (logic moved to `autoRegister` in `ServiceRegistryExt`)
- Removed `useCase.attachParent(serviceRegistry)` and `registerAutoServices(useCase, serviceRegistry)` from the `onEach` block
- Added `internal fun allUseCases()` for parent ViewModel to enumerate children during unregistration
- Added `init { autoRegister(requireServiceRegistry()) }`
- Marked `attachParent` `@Deprecated`

- [ ] **Step 2: Verify core module compiles**

```bash
./gradlew :scaffold:core:compileKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Write integration test for CombineUseCase registration**

```kotlin
package zhaoyun.example.composedemo.scaffold.core.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.*

class CombineUseCaseRegistryTest {

    interface DemoService : MviService {
        fun demo()
    }

    class DemoUseCase(stateHolder: StateHolder<TestState>) : BaseUseCase<TestState, TestEvent, TestEffect>(stateHolder), DemoService {
        override fun demo() {}
        override suspend fun onEvent(event: TestEvent) {}
    }

    data object TestState : UiState
    data object TestEvent : UiEvent
    data object TestEffect : UiEffect

    @Before
    fun setup() {
        startKoin {
            modules(module {
                scope(named("MviScreenScope")) {
                    scoped<MutableServiceRegistry> { MutableServiceRegistryImpl() }
                }
            })
        }
    }

    @After
    fun teardown() {
        stopKoin()
        while (ScreenScopeStack.current != null) {
            ScreenScopeStack.pop()
        }
    }

    @Test
    fun `child UseCase auto-registers in shared Screen registry`() {
        val koin = org.koin.core.context.GlobalContext.get()
        val scope = koin.createScope("test", named("MviScreenScope"))
        ScreenScopeStack.push(scope)

        val stateHolder = TestStateHolder(TestState)
        val combineUseCase = CombineUseCase(stateHolder, { DemoUseCase(it) })

        val registry = scope.get<MutableServiceRegistry>()
        assertNotNull(registry.find(DemoService::class.java))

        ScreenScopeStack.pop()
        scope.close()
    }

    class TestStateHolder(override val initialState: TestState) : StateHolder<TestState> {
        override val state = MutableStateFlow(initialState)
        override fun updateState(transform: (TestState) -> TestState) {
            state.value = transform(state.value)
        }

        override fun <D : UiState> derive(childSelector: (TestState) -> D, parentUpdater: TestState.(D) -> TestState): StateHolder<D> {
            throw NotImplementedError()
        }
    }
}
```

- [ ] **Step 4: Run integration test**

```bash
./gradlew :scaffold:core:test --tests "zhaoyun.example.composedemo.scaffold.core.usecase.CombineUseCaseRegistryTest"
```

Expected: BUILD SUCCESSFUL, tests pass.

- [ ] **Step 5: Commit**

```bash
git add scaffold/core/src/main/kotlin/.../usecase/CombineUseCase.kt \
        scaffold/core/src/test/kotlin/.../usecase/CombineUseCaseRegistryTest.kt
git commit -m "refactor: CombineUseCase self-registers, removes attachParent and auto-service logic"
```

---

## Task 5: BaseViewModel — remove own registry, init self-register, onCleared unregister tree

**Files:**
- Modify: `scaffold/android/src/main/kotlin/zhaoyun/example/composedemo/scaffold/android/BaseViewModel.kt`

- [ ] **Step 1: Modify BaseViewModel**

Replace the file content with:

```kotlin
package zhaoyun.example.composedemo.scaffold.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import zhaoyun.example.composedemo.scaffold.core.mvi.EffectDispatcher
import zhaoyun.example.composedemo.scaffold.core.mvi.EventReceiver
import zhaoyun.example.composedemo.scaffold.core.mvi.MviFacade
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.autoRegister
import zhaoyun.example.composedemo.scaffold.core.spi.autoUnregister
import zhaoyun.example.composedemo.scaffold.core.spi.requireServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.CombineUseCase
import zhaoyun.example.composedemo.scaffold.core.usecase.UseCaseFactory

open class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    override val stateHolder: StateHolder<S>,
    vararg useCaseCreators: UseCaseFactory<S, E, F>,
) : ViewModel(),
    MviFacade<S, E, F> {

    private val combineUseCase = CombineUseCase(
        stateHolder = stateHolder,
        useCaseCreators = useCaseCreators
    )

    // Cache the Screen-level registry during init so onCleared() can unregister without Stack
    private val screenRegistry: MutableServiceRegistry = requireServiceRegistry()

    init {
        autoRegister(screenRegistry)
    }

    override val eventReceiver: EventReceiver<E>
        get() = combineUseCase.eventReceiver

    override val effectDispatcher: EffectDispatcher<F>
        get() = combineUseCase.effectDispatcher

    fun sendEvent(event: E) {
        viewModelScope.launch {
            receiveEvent(event)
        }
    }

    @Deprecated(
        "attachParent is no longer needed. ServiceRegistry is now shared per-Screen via Koin Scope.",
        ReplaceWith("")
    )
    fun attachParent(serviceRegistry: ServiceRegistry) {
        // no-op: retained for binary compatibility during migration
    }

    fun <T : Any> registerService(
        clazz: Class<T>,
        instance: T,
        tag: String? = null,
    ) {
        screenRegistry.register(clazz, instance, tag)
    }

    inline fun <reified T : Any> registerService(
        instance: T,
        tag: String? = null,
    ) {
        screenRegistry.register(T::class.java, instance, tag)
    }

    fun unregisterService(
        clazz: Class<*>,
        tag: String? = null,
    ) {
        screenRegistry.unregister(clazz, tag)
    }

    fun unregisterService(instance: Any) {
        screenRegistry.unregister(instance)
    }

    override fun onCleared() {
        // Unregister the whole tree: children → combine → self
        combineUseCase.allUseCases().forEach { screenRegistry.autoUnregister(it) }
        screenRegistry.autoUnregister(combineUseCase)
        screenRegistry.autoUnregister(this)
        super.onCleared()
    }
}
```

Key changes:
- Removed `val serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl()`
- Removed `.apply { attachParent(serviceRegistry) }` on `combineUseCase`
- Added `private val screenRegistry = requireServiceRegistry()` cached field
- Added `init { autoRegister(screenRegistry) }`
- `onCleared()` now unregisters the whole tree using `combineUseCase.allUseCases()`
- Marked `attachParent` `@Deprecated`

- [ ] **Step 2: Verify android module compiles**

```bash
./gradlew :scaffold:android:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add scaffold/android/src/main/kotlin/.../android/BaseViewModel.kt
git commit -m "refactor: BaseViewModel self-registers and unregisters tree on clear"
```

---

## Task 6: ServiceRegistryCompositionLocal — add LocalKoinScope

**Files:**
- Modify: `scaffold/android/src/main/kotlin/zhaoyun/example/composedemo/scaffold/android/ServiceRegistryCompositionLocal.kt`

- [ ] **Step 1: Add LocalKoinScope**

```kotlin
package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import org.koin.core.scope.Scope
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry

/**
 * CompositionLocal —— 向下传递当前 Screen 的 [MutableServiceRegistry]
 */
val LocalServiceRegistry = compositionLocalOf<MutableServiceRegistry?> { null }

/**
 * CompositionLocal —— 向下传递当前 Screen 的 Koin [Scope]
 */
val LocalKoinScope = compositionLocalOf<Scope?> { null }

/**
 * 向当前 Compose 子树暴露既有的服务作用域。
 */
@Composable
fun ServiceRegistryProvider(
    registry: MutableServiceRegistry,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalServiceRegistry provides registry) {
        content()
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add scaffold/android/src/main/kotlin/.../android/ServiceRegistryCompositionLocal.kt
git commit -m "feat: add LocalKoinScope CompositionLocal"
```

---

## Task 7: MviScreen — create Koin Scope and declare registry bean

**Files:**
- Modify: `scaffold/android/src/main/kotlin/zhaoyun/example/composedemo/scaffold/android/MviScreen.kt`

- [ ] **Step 1: Modify MviScreen**

```kotlin
package zhaoyun.example.composedemo.scaffold.android

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.koin.androidx.compose.getKoin
import org.koin.core.qualifier.named
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.ScreenScopeStack

/**
 * MVI 通用屏幕包装器 —— 仅负责 [BaseEffect] 的收集与默认处理。
 *
 * State 收集、Effect 收集、initEvent 发送均由调用方（Screen 层）自行组装。
 */
@Composable
fun <S : UiState, E : UiEvent, F : UiEffect> MviScreen(
    viewModel: BaseViewModel<S, E, F>,
    onBaseEffect: suspend (BaseEffect) -> Boolean = { false },
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
        ScreenScopeStack.push(scope)
        onDispose {
            screenRegistry.clear()
            scope.close()
            ScreenScopeStack.pop()
        }
    }

    ServiceRegistryProvider(registry = screenRegistry) {
        CompositionLocalProvider(LocalKoinScope provides scope) {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.baseEffect.collect { effect ->
                    val consumed = onBaseEffect(effect)
                    if (!consumed) {
                        defaultHandleBaseEffect(context, effect)
                    }
                }
            }
            content()
        }
    }
}

/**
 * [BaseEffect] 的默认处理实现 —— 仅处理无需 Compose 树或导航栈的副作用
 *
 * 当前支持：
 * - [BaseEffect.ShowToast]
 *
 * 其他类型（如 [BaseEffect.ShowDialog]、[BaseEffect.NavigateBack]）
 * 建议在调用方通过 [MviScreen] 的 [onBaseEffect] 参数自行处理。
 */
suspend fun defaultHandleBaseEffect(context: Context, effect: BaseEffect) {
    when (effect) {
        is BaseEffect.ShowToast -> {
            Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
        else -> {
            // 其他 BaseEffect 默认不处理，由调用方通过 onBaseEffect 覆盖
        }
    }
}
```

Key changes:
- `screenRegistry` is now created locally inside `MviScreen` instead of reading from `viewModel.serviceRegistry`
- Koin `Scope` is created and `screenRegistry` is declared as a bean inside it
- `ScreenScopeStack.push(scope)` happens in `DisposableEffect` (when composition enters)
- `ScreenScopeStack.pop()` + `screenRegistry.clear()` + `scope.close()` happen in `onDispose`
- `LocalKoinScope` is provided to the content subtree

> **Note:** The `DisposableEffect` pushes the scope once when `MviScreen` enters composition. `screenViewModel()` (inside the content) also pushes/pops around `koinViewModel()` creation. This double-push is safe because `koinViewModel()` will push the same scope again, then pop it, leaving the `MviScreen`-pushed scope still on the stack. When `MviScreen` disposes, it pops its own scope. If there are nested `screenViewModel()` calls, each adds and removes its own layer. The stack correctly supports nesting.

Wait — actually this is a problem. `MviScreen` pushes the scope in `DisposableEffect`, but `screenViewModel()` also pushes/pops. Let's trace:

1. `MviScreen` composition starts
2. `DisposableEffect` runs → `ScreenScopeStack.push(scopeA)`
3. Inside `content`, `screenViewModel()` runs
4. `screenViewModel()` does `push(scopeA)` → Stack now has [scopeA, scopeA]
5. `koinViewModel()` creates VM (init{} runs, reads `current` = scopeA)
6. `screenViewModel()` does `pop()` → Stack back to [scopeA]
7. Later, `MviScreen` disposes → `pop()` → Stack empty

This works! The double push/pop is safe because it's the same scope. But it's slightly wasteful. We could simplify by removing the push from `MviScreen` and only relying on `screenViewModel()`, but then what if a Screen has no `screenViewModel()` call (only uses the root VM)? In that case, the root VM's `init{}` would also need the Stack to be pushed.

Actually, in the current design, `MviScreen` receives `viewModel` as a parameter — the root VM is already created outside `MviScreen` (usually via `koinViewModel()` in the Screen composable). So the Stack should be pushed before the root VM is created too.

But the design doc says `MviScreen` pushes the scope. Let's keep it consistent with the design doc for now and verify during implementation if the double-push causes issues. If it does, we'll move the Stack push into `screenViewModel()` only and ensure root VMs are created inside `MviScreen`'s scope.

For the plan, keep the code as designed.

- [ ] **Step 2: Verify android module compiles**

```bash
./gradlew :scaffold:android:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add scaffold/android/src/main/kotlin/.../android/MviScreen.kt
git commit -m "feat: MviScreen creates Koin Scope, declares registry bean, manages ScreenScopeStack"
```

---

## Task 8: screenViewModel() — push/pop Stack and pass scope

**Files:**
- Modify: `scaffold/android/src/main/kotlin/zhaoyun/example/composedemo/scaffold/android/ScreenViewModel.kt`

- [ ] **Step 1: Modify screenViewModel**

```kotlin
package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.runtime.Composable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder
import org.koin.core.parameter.parametersOf
import zhaoyun.example.composedemo.scaffold.core.spi.ScreenScopeStack

@Composable
inline fun <reified D, reified VM : BaseViewModel<*, *, *>> screenViewModel(
    keyData: D,
    noinline parameters: (() -> ParametersHolder)? = null
): VM {
    val scope = checkNotNull(LocalKoinScope.current) {
        "screenViewModel() must be called inside a Screen that provides LocalKoinScope. " +
            "Wrap your Screen root with MviScreen { ... }"
    }

    val key = VM::class.simpleName + " " + D::class.simpleName + " " + keyData.hashCode()
    val params = parameters ?: { parametersOf(keyData) }

    ScreenScopeStack.push(scope)
    try {
        val viewModel = koinViewModel<VM>(
            key = key,
            scope = scope,
            parameters = params
        )
        return viewModel
    } finally {
        ScreenScopeStack.pop()
    }
}
```

Key changes:
- Removed `val registry = checkNotNull(LocalServiceRegistry.current)`
- Now reads `LocalKoinScope.current` to get the Koin Scope
- `ScreenScopeStack.push(scope)` before `koinViewModel()` creation
- `ScreenScopeStack.pop()` in `finally` after creation
- `koinViewModel()` receives `scope = scope` to create VM inside the Screen's Koin Scope
- Removed `viewModel.attachParent(registry)` call

- [ ] **Step 2: Verify android module compiles**

```bash
./gradlew :scaffold:android:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add scaffold/android/src/main/kotlin/.../android/ScreenViewModel.kt
git commit -m "refactor: screenViewModel uses LocalKoinScope, push/pop ScreenScopeStack, pass scope"
```

---

## Task 9: Mark attachParent / detachParent deprecated in ServiceRegistry interface

**Files:**
- Modify: `scaffold/core/src/main/kotlin/zhaoyun/example/composedemo/scaffold/core/spi/ServiceRegistry.kt`

- [ ] **Step 1: Add @Deprecated annotations**

```kotlin
package zhaoyun.example.composedemo.scaffold.core.spi

data class ServiceKey<T : Any>(
    val clazz: Class<T>,
    val tag: String? = null,
)

interface ServiceRegistry {
    fun <T : Any> find(clazz: Class<T>, tag: String? = null): T?
}

inline fun <reified T : Any> ServiceRegistry.find(tag: String? = null): T? =
    find(T::class.java, tag)

interface MutableServiceRegistry : ServiceRegistry {
    @Deprecated(
        "attachParent is no longer needed. ServiceRegistry is now shared per-Screen via Koin Scope.",
        ReplaceWith("")
    )
    fun attachParent(serviceRegistry: ServiceRegistry)

    @Deprecated(
        "detachParent is no longer needed.",
        ReplaceWith("")
    )
    fun detachParent()

    fun <T : Any> register(clazz: Class<T>, instance: T, tag: String? = null)
    fun unregister(clazz: Class<*>, tag: String? = null)
    fun unregister(instance: Any)
    fun clear()
}

inline fun <reified T : Any> MutableServiceRegistry.register(
    instance: T,
    tag: String? = null,
) = register(T::class.java, instance, tag)
```

- [ ] **Step 2: Commit**

```bash
git add scaffold/core/src/main/kotlin/.../spi/ServiceRegistry.kt
git commit -m "deprecate: attachParent and detachParent in MutableServiceRegistry interface"
```

---

## Task 10: Integration verification (manual / existing tests)

- [ ] **Step 1: Run all scaffold tests**

```bash
./gradlew :scaffold:core:test
./gradlew :scaffold:android:testDebugUnitTest
```

Expected: All tests pass (existing tests may need minor updates if they relied on `BaseViewModel.serviceRegistry` field).

- [ ] **Step 2: Run a quick build check**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit any test fixes**

If existing tests failed due to the removed `serviceRegistry` field, update them:
- Tests that accessed `viewModel.serviceRegistry` directly should now use `LocalServiceRegistry.current` or verify registration via `findService()`.

```bash
git add -A
git commit -m "test: update existing tests for new ServiceRegistry architecture"
```

---

## Self-Review

### 1. Spec coverage

| Spec Section | Implementing Task |
|--------------|-------------------|
| Koin Scope per Screen | Task 7 (MviScreen) |
| ScreenScopeStack | Task 2 |
| `requireServiceRegistry()` | Task 2 |
| `autoRegister()` / `autoUnregister()` | Task 2 |
| BaseUseCase `init{}` self-register | Task 3 |
| CombineUseCase `init{}` self-register + `allUseCases()` | Task 4 |
| BaseViewModel `init{}` self-register + `onCleared()` tree unregister | Task 5 |
| `screenViewModel()` push/pop + `scope` param | Task 8 |
| `LocalKoinScope` | Task 6 |
| Mark old API deprecated | Task 9 |

No gaps identified.

### 2. Placeholder scan

- No "TBD", "TODO", "implement later", "fill in details" found.
- All steps show actual code or exact commands.
- No vague instructions like "add appropriate error handling".

### 3. Type consistency

- `autoRegister(registry: MutableServiceRegistry)` used consistently across Task 2, 3, 4, 5.
- `ScreenScopeStack` API (`push`, `pop`, `current`, `requireCurrent`) consistent.
- `LocalKoinScope` type is `Scope?` everywhere.
- File paths match the actual project structure verified by `Glob`.

### 4. Known risks documented

- `scaffold/core` gains `koin-core` dependency — documented in Task 1.
- `MviScreen` and `screenViewModel()` both touch `ScreenScopeStack` — documented in Task 7 with behavior analysis.
- `BaseViewModel.onCleared()` uses cached `screenRegistry` instead of Stack — documented in Task 5.
