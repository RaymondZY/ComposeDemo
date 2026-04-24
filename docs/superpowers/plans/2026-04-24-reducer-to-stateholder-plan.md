# Reducer → StateHolder 全局重命名 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 MVI 框架中所有 `Reducer` 相关命名替换为 `StateHolder` / `update`，不留废弃别名，不引入逻辑变更。

**Architecture:** 纯命名重构。先改 `:scaffold:core` 接口层，再改 `:scaffold:android` 基类层，最后级联到所有业务模块的 ViewModel、DI Module、测试与文档。每批次修改后立即编译验证。

**Tech Stack:** Kotlin, Gradle, Koin, Jetpack Compose, JUnit

---

## Task 1: 核心接口与 BaseUseCase（`:scaffold:core`）

**Files:**
- Rename: `scaffold/core/src/main/kotlin/zhaoyun/example/composedemo/scaffold/core/mvi/Reducer.kt` → `StateHolder.kt`
- Modify: `scaffold/core/src/main/kotlin/zhaoyun/example/composedemo/scaffold/core/mvi/BaseUseCase.kt`

- [ ] **Step 1: 重命名 `Reducer.kt` 并替换内部所有命名**

将文件重命名为 `StateHolder.kt`，内容替换为：

```kotlin
package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 状态读写最小契约 —— 决定「状态存在哪里」以及「如何更新它」
 *
 * [BaseViewModel] 与 [BaseUseCase] 均面向此接口编程，
 * 从而实现「独立持态」与「代理到外部」两种模式的无感知切换。
 */
interface StateHolder<S> {
    val state: StateFlow<S>
    fun update(transform: (S) -> S)
}

/**
 * 本地 StateHolder —— 内部持有 [MutableStateFlow]，作为独立页面时的默认实现
 */
class LocalStateHolder<S>(initial: S) : StateHolder<S> {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<S> = _state.asStateFlow()
    override fun update(transform: (S) -> S) {
        _state.update(transform)
    }
}

/**
 * 代理 StateHolder —— 将状态读写代理到外部提供的 [StateFlow] 与 [onUpdate] 回调
 *
 * 典型使用场景：直接创建 [DelegateStateHolder] 实例注入给 Detail ViewModel，
 * 使 DetailState 成为 GlobalState 的结构性子集。
 */
class DelegateStateHolder<S>(
    override val state: StateFlow<S>,
    private val onUpdate: ((S) -> S) -> Unit
) : StateHolder<S> {
    override fun update(transform: (S) -> S) = onUpdate(transform)
}
```

- [ ] **Step 2: 修改 `BaseUseCase.kt`**

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

    abstract suspend fun onEvent(event: E)
}
```

- [ ] **Step 3: 编译验证 `:scaffold:core`**

Run: `./gradlew :scaffold:core:compileKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add scaffold/core/src/main/kotlin/zhaoyun/example/composedemo/scaffold/core/mvi/
git commit -m "refactor(scaffold-core): rename Reducer to StateHolder, reduce() to update()"
```

---

## Task 2: ViewModel 基类（`:scaffold:android`）

**Files:**
- Modify: `scaffold/android/src/main/kotlin/zhaoyun/example/composedemo/scaffold/android/BaseViewModel.kt`

- [ ] **Step 1: 修改 `BaseViewModel.kt`**

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
}
```

- [ ] **Step 2: 编译验证 `:scaffold:android`**

Run: `./gradlew :scaffold:android:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add scaffold/android/src/main/kotlin/zhaoyun/example/composedemo/scaffold/android/BaseViewModel.kt
git commit -m "refactor(scaffold-android): adapt BaseViewModel to StateHolder naming"
```

---

## Task 3: `:scaffold:core` 单元测试

**Files:**
- Rename: `scaffold/core/src/test/kotlin/zhaoyun/example/composedemo/scaffold/core/mvi/ReducerTest.kt` → `StateHolderTest.kt`
- Rename: `scaffold/core/src/test/kotlin/zhaoyun/example/composedemo/scaffold/core/mvi/BaseUseCaseReducerBindTest.kt` → `BaseUseCaseStateHolderBindTest.kt`
- Rename: `scaffold/core/src/test/kotlin/zhaoyun/example/composedemo/scaffold/core/mvi/GlobalReducerIntegrationTest.kt` → `GlobalStateHolderIntegrationTest.kt`

- [ ] **Step 1: 替换 `ReducerTest.kt` 为 `StateHolderTest.kt`**

```kotlin
package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StateHolderTest {

    data class TestState(val count: Int = 0) : UiState

    @Test
    fun `LocalStateHolder执行update后状态正确更新`() = runTest {
        val stateHolder = LocalStateHolder(TestState(0))
        assertEquals(0, stateHolder.state.value.count)

        stateHolder.update { it.copy(count = it.count + 1) }
        assertEquals(1, stateHolder.state.value.count)

        stateHolder.update { it.copy(count = it.count + 5) }
        assertEquals(6, stateHolder.state.value.count)
    }

    @Test
    fun `LocalStateHolder并发update保证原子性`() = runTest {
        val stateHolder = LocalStateHolder(TestState(0))
        repeat(100) {
            stateHolder.update { it.copy(count = it.count + 1) }
        }
        assertEquals(100, stateHolder.state.value.count)
    }

    @Test
    fun `DelegateStateHolder的onUpdate被正确触发`() = runTest {
        val external = MutableStateFlow(TestState(10))
        var receivedTransform: ((TestState) -> TestState)? = null

        val stateHolder = DelegateStateHolder(
            state = external,
            onUpdate = { transform ->
                receivedTransform = transform
                external.value = transform(external.value)
            }
        )

        stateHolder.update { it.copy(count = it.count + 3) }

        assertEquals(13, external.value.count)
        assertEquals(13, stateHolder.state.first().count)
        assertEquals(13, receivedTransform?.invoke(TestState(10))?.count)
    }

    @Test
    fun `DelegateStateHolder的stateFlow与外部源同步`() = runTest {
        val external = MutableStateFlow(TestState(5))
        val stateHolder = DelegateStateHolder(
            state = external,
            onUpdate = { transform -> external.value = transform(external.value) }
        )

        assertEquals(5, stateHolder.state.value.count)

        external.value = TestState(20)
        assertEquals(20, stateHolder.state.value.count)
    }
}
```

- [ ] **Step 2: 替换 `BaseUseCaseReducerBindTest.kt` 为 `BaseUseCaseStateHolderBindTest.kt`**

```kotlin
package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BaseUseCaseStateHolderBindTest {

    data class TestState(val value: Int = 0) : UiState
    object TestEvent : UiEvent
    object TestEffect : UiEffect

    class TestUseCase : BaseUseCase<TestState, TestEvent, TestEffect>(TestState(0)) {
        fun increment() = updateState { it.copy(value = it.value + 1) }
        fun getCurrent(): TestState = currentState

        override suspend fun onEvent(event: TestEvent) {
            // no-op for test
        }
    }

    @Test
    fun `未bind时使用internalState`() {
        val useCase = TestUseCase()
        assertEquals(0, useCase.state.value.value)

        useCase.increment()
        assertEquals(1, useCase.state.value.value)
        assertEquals(1, useCase.getCurrent().value)
    }

    @Test
    fun `bind后updateState路由到stateHolder`() = runTest {
        val useCase = TestUseCase()
        val stateHolder = LocalStateHolder(TestState(100))

        useCase.bind(stateHolder)
        assertEquals(100, useCase.state.value.value)

        useCase.increment()
        assertEquals(101, useCase.state.value.value)
        assertEquals(101, stateHolder.state.value.value)
        assertEquals(101, useCase.getCurrent().value)
    }

    @Test
    fun `bind后多个useCase共享同一个stateHolder`() = runTest {
        val stateHolder = LocalStateHolder(TestState(0))
        val useCaseA = TestUseCase()
        val useCaseB = TestUseCase()

        useCaseA.bind(stateHolder)
        useCaseB.bind(stateHolder)

        useCaseA.increment()
        assertEquals(1, useCaseA.state.value.value)
        assertEquals(1, useCaseB.state.value.value)

        useCaseB.increment()
        assertEquals(2, useCaseA.state.value.value)
        assertEquals(2, useCaseB.state.value.value)
    }
}
```

- [ ] **Step 3: 替换 `GlobalReducerIntegrationTest.kt` 为 `GlobalStateHolderIntegrationTest.kt`**

```kotlin
package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Global与DetailStateHolder集成测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GlobalStateHolderIntegrationTest {

    data class DetailState(val count: Int, val label: String) : UiState
    data class GlobalState(
        val detail: DetailState,
        val totalUpdates: Int = 0
    )

    @Test
    fun `DetailStateHolder通过DelegateStateHolder正确写回GlobalState`() = runTest {
        val globalState = MutableStateFlow(
            GlobalState(detail = DetailState(0, "A"))
        )

        val detailStateHolder = DelegateStateHolder(
            state = MutableStateFlow(globalState.value.detail),
            onUpdate = { transform ->
                globalState.value = globalState.value.copy(
                    detail = transform(globalState.value.detail)
                )
            }
        )

        detailStateHolder.update { it.copy(count = it.count + 1) }
        assertEquals(1, globalState.value.detail.count)
        assertEquals("A", globalState.value.detail.label)

        detailStateHolder.update { it.copy(label = "B") }
        assertEquals(1, globalState.value.detail.count)
        assertEquals("B", globalState.value.detail.label)
    }

    @Test
    fun `StateHolder拦截器可在Detail更新时同步修改Global其他字段`() = runTest {
        val globalState = MutableStateFlow(
            GlobalState(detail = DetailState(0, "A"), totalUpdates = 0)
        )

        val detailStateHolder = DelegateStateHolder(
            state = MutableStateFlow(globalState.value.detail),
            onUpdate = { transform ->
                val newDetail = transform(globalState.value.detail)
                globalState.value = globalState.value.copy(
                    detail = newDetail,
                    totalUpdates = globalState.value.totalUpdates + 1
                )
            }
        )

        detailStateHolder.update { it.copy(count = 1) }
        assertEquals(1, globalState.value.totalUpdates)

        detailStateHolder.update { it.copy(count = 2) }
        assertEquals(2, globalState.value.totalUpdates)

        detailStateHolder.update { it.copy(label = "Z") }
        assertEquals(3, globalState.value.totalUpdates)
    }

    @Test
    fun `多个DetailStateHolder代理到同一个GlobalState的不同切片`() = runTest {
        data class SliceA(val value: Int) : UiState
        data class SliceB(val value: Int) : UiState
        data class MultiGlobalState(val a: SliceA, val b: SliceB)

        val globalState = MutableStateFlow(MultiGlobalState(SliceA(0), SliceB(0)))

        val stateHolderA = DelegateStateHolder(
            state = MutableStateFlow(globalState.value.a),
            onUpdate = { transform ->
                globalState.value = globalState.value.copy(
                    a = transform(globalState.value.a)
                )
            }
        )

        val stateHolderB = DelegateStateHolder(
            state = MutableStateFlow(globalState.value.b),
            onUpdate = { transform ->
                globalState.value = globalState.value.copy(
                    b = transform(globalState.value.b)
                )
            }
        )

        stateHolderA.update { it.copy(value = 10) }
        stateHolderB.update { it.copy(value = 20) }

        assertEquals(10, globalState.value.a.value)
        assertEquals(20, globalState.value.b.value)
    }
}
```

- [ ] **Step 4: 删除旧测试文件并运行 `:scaffold:core` 测试**

Run:
```bash
rm scaffold/core/src/test/kotlin/zhaoyun/example/composedemo/scaffold/core/mvi/ReducerTest.kt
rm scaffold/core/src/test/kotlin/zhaoyun/example/composedemo/scaffold/core/mvi/BaseUseCaseReducerBindTest.kt
rm scaffold/core/src/test/kotlin/zhaoyun/example/composedemo/scaffold/core/mvi/GlobalReducerIntegrationTest.kt
./gradlew :scaffold:core:test
```
Expected: BUILD SUCCESSFUL, all tests pass

- [ ] **Step 5: Commit**

```bash
git add scaffold/core/src/test/
git commit -m "test(scaffold-core): rename Reducer tests to StateHolder tests"
```

---

## Task 4: `:scaffold:android` Android 测试

**Files:**
- Rename: `scaffold/android/src/androidTest/kotlin/zhaoyun/example/composedemo/scaffold/android/MviScreenReducerIntegrationTest.kt` → `MviScreenStateHolderIntegrationTest.kt`

- [ ] **Step 1: 替换测试文件**

```kotlin
package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase
import zhaoyun.example.composedemo.scaffold.core.mvi.DelegateStateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

/**
 * MviScreen与StateHolder架构集成测试
 */
@RunWith(AndroidJUnit4::class)
class MviScreenStateHolderIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    data class TestState(val text: String) : UiState
    data class TestEvent(val newText: String) : UiEvent
    object TestEffect : UiEffect

    class TestUseCase : BaseUseCase<TestState, TestEvent, TestEffect>(TestState("initial")) {
        override suspend fun onEvent(event: TestEvent) {
            updateState { it.copy(text = event.newText) }
        }
    }

    class LocalTestViewModel : BaseViewModel<TestState, TestEvent, TestEffect>(
        initialState = TestState("initial"),
        null,
        TestUseCase()
    )

    class DelegateTestViewModel(private val injectedStateHolder: StateHolder<TestState>) : BaseViewModel<TestState, TestEvent, TestEffect>(
        initialState = TestState("initial"),
        injectedStateHolder,
        TestUseCase()
    )

    @Composable
    fun TestScreen(state: TestState, onEvent: (TestEvent) -> Unit) {
        BasicText(text = state.text)
    }

    @Test
    fun `LocalStateHolder模式下MviScreen正确收集State`() {
        val viewModel = LocalTestViewModel()

        composeTestRule.setContent {
            MviScreen(viewModel = viewModel) { state, onEvent ->
                TestScreen(state = state, onEvent = onEvent)
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("initial").assertIsDisplayed()

        viewModel.onEvent(TestEvent("updated"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("updated").assertIsDisplayed()
    }

    @Test
    fun `DelegateStateHolder模式下MviScreen正确收集State`() {
        val externalState = MutableStateFlow(TestState("external"))
        val stateHolder = DelegateStateHolder(
            state = externalState,
            onUpdate = { transform -> externalState.value = transform(externalState.value) }
        )
        val viewModel = DelegateTestViewModel(stateHolder)

        composeTestRule.setContent {
            MviScreen(viewModel = viewModel) { state, onEvent ->
                TestScreen(state = state, onEvent = onEvent)
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("external").assertIsDisplayed()

        viewModel.onEvent(TestEvent("delegated"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("delegated").assertIsDisplayed()
    }
}
```

- [ ] **Step 2: 删除旧文件并编译验证**

Run:
```bash
rm scaffold/android/src/androidTest/kotlin/zhaoyun/example/composedemo/scaffold/android/MviScreenReducerIntegrationTest.kt
./gradlew :scaffold:android:compileDebugAndroidTest
```
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add scaffold/android/src/androidTest/
git commit -m "test(scaffold-android): rename MviScreenReducerIntegrationTest to StateHolder"
```

---

## Task 5: `:biz:story` 模块（ViewModel + Page + DI）

**Files:**
- Modify: `biz/story/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/presentation/StoryCardViewModel.kt`
- Modify: `biz/story/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/presentation/StoryCardPage.kt`
- Modify: `biz/story/message/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/message/presentation/MessageViewModel.kt`
- Modify: `biz/story/message/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/message/presentation/di/MessagePresentationModule.kt`
- Modify: `biz/story/input/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/input/presentation/InputViewModel.kt`
- Modify: `biz/story/input/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/input/presentation/di/InputPresentationModule.kt`
- Modify: `biz/story/infobar/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/infobar/presentation/InfoBarViewModel.kt`
- Modify: `biz/story/infobar/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/infobar/presentation/di/InfoBarPresentationModule.kt`
- Modify: `biz/story/background/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/background/presentation/BackgroundViewModel.kt`
- Modify: `biz/story/background/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/background/presentation/di/BackgroundPresentationModule.kt`

- [ ] **Step 1: `StoryCardViewModel.kt`**

```kotlin
package zhaoyun.example.composedemo.story.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.background.domain.BackgroundState
import zhaoyun.example.composedemo.story.domain.StoryCardEffect
import zhaoyun.example.composedemo.story.domain.StoryCardEvent
import zhaoyun.example.composedemo.story.domain.StoryCardState
import zhaoyun.example.composedemo.story.domain.StoryCardUseCase
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarState
import zhaoyun.example.composedemo.story.input.domain.InputState
import zhaoyun.example.composedemo.story.message.domain.MessageState

class StoryCardViewModel : BaseViewModel<StoryCardState, StoryCardEvent, StoryCardEffect>(
    StoryCardState(),
    null,
    StoryCardUseCase()
) {
    val messageStateHolder: StateHolder<MessageState> by lazy {
        createDelegateStateHolder(StoryCardState::message) { storyCardState, state ->
            storyCardState.copy(message = state)
        }
    }
    val infoBarStateHolder: StateHolder<InfoBarState> by lazy {
        createDelegateStateHolder(StoryCardState::infoBar) { storyCardState, state ->
            storyCardState.copy(infoBar = state)
        }
    }
    val inputStateHolder: StateHolder<InputState> by lazy {
        createDelegateStateHolder(StoryCardState::input) { storyCardState, state ->
            storyCardState.copy(input = state)
        }
    }
    val backgroundStateHolder: StateHolder<BackgroundState> by lazy {
        createDelegateStateHolder(StoryCardState::background) { storyCardState, state ->
            storyCardState.copy(background = state)
        }
    }
}
```

- [ ] **Step 2: `StoryCardPage.kt`**

```kotlin
package zhaoyun.example.composedemo.story.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
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
    val storyViewModel: StoryCardViewModel = koinViewModel()

    val messageViewModel: MessageViewModel = koinViewModel {
        parametersOf(storyViewModel.messageStateHolder)
    }
    val infoBarViewModel: InfoBarViewModel = koinViewModel {
        parametersOf(storyViewModel.infoBarStateHolder, card.cardId)
    }
    val inputViewModel: InputViewModel = koinViewModel {
        parametersOf(storyViewModel.inputStateHolder)
    }
    val backgroundViewModel: BackgroundViewModel = koinViewModel {
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
```

- [ ] **Step 3: `MessageViewModel.kt` 与 `MessagePresentationModule.kt`**

`MessageViewModel.kt`:
```kotlin
package zhaoyun.example.composedemo.story.message.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.message.domain.MessageEffect
import zhaoyun.example.composedemo.story.message.domain.MessageEvent
import zhaoyun.example.composedemo.story.message.domain.MessageState
import zhaoyun.example.composedemo.story.message.domain.MessageUseCase

class MessageViewModel(
    messageStateHolder: StateHolder<MessageState>,
) : BaseViewModel<MessageState, MessageEvent, MessageEffect>(
    MessageState(),
    messageStateHolder,
    MessageUseCase()
)
```

`MessagePresentationModule.kt`:
```kotlin
package zhaoyun.example.composedemo.story.message.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.message.domain.MessageState
import zhaoyun.example.composedemo.story.message.presentation.MessageViewModel

val messagePresentationModule = module {
    viewModel { (stateHolder: StateHolder<MessageState>) ->
        MessageViewModel(messageStateHolder = stateHolder)
    }
}
```

- [ ] **Step 4: `InputViewModel.kt` 与 `InputPresentationModule.kt`**

`InputViewModel.kt`:
```kotlin
package zhaoyun.example.composedemo.story.input.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.input.domain.InputEffect
import zhaoyun.example.composedemo.story.input.domain.InputEvent
import zhaoyun.example.composedemo.story.input.domain.InputState
import zhaoyun.example.composedemo.story.input.domain.InputUseCase

class InputViewModel(
    inputStateHolder: StateHolder<InputState>,
) : BaseViewModel<InputState, InputEvent, InputEffect>(
    InputState(),
    inputStateHolder,
    InputUseCase()
)
```

`InputPresentationModule.kt`:
```kotlin
package zhaoyun.example.composedemo.story.input.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.input.domain.InputState
import zhaoyun.example.composedemo.story.input.presentation.InputViewModel

val inputPresentationModule = module {
    viewModel { (stateHolder: StateHolder<InputState>) ->
        InputViewModel(inputStateHolder = stateHolder)
    }
}
```

- [ ] **Step 5: `InfoBarViewModel.kt` 与 `InfoBarPresentationModule.kt`**

`InfoBarViewModel.kt`:
```kotlin
package zhaoyun.example.composedemo.story.infobar.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarEffect
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarEvent
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarState
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarUseCase

class InfoBarViewModel(
    stateHolder: StateHolder<InfoBarState>,
    cardId: String,
) : BaseViewModel<InfoBarState, InfoBarEvent, InfoBarEffect>(
    InfoBarState(),
    stateHolder,
    InfoBarUseCase(cardId)
)
```

`InfoBarPresentationModule.kt`:
```kotlin
package zhaoyun.example.composedemo.story.infobar.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarState
import zhaoyun.example.composedemo.story.infobar.presentation.InfoBarViewModel

val infoBarPresentationModule = module {
    viewModel { (stateHolder: StateHolder<InfoBarState>, cardId: String) ->
        InfoBarViewModel(stateHolder = stateHolder, cardId = cardId)
    }
}
```

- [ ] **Step 6: `BackgroundViewModel.kt` 与 `BackgroundPresentationModule.kt`**

`BackgroundViewModel.kt`:
```kotlin
package zhaoyun.example.composedemo.story.background.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.background.domain.BackgroundEffect
import zhaoyun.example.composedemo.story.background.domain.BackgroundEvent
import zhaoyun.example.composedemo.story.background.domain.BackgroundState
import zhaoyun.example.composedemo.story.background.domain.BackgroundUseCase

class BackgroundViewModel(
    backgroundStateHolder: StateHolder<BackgroundState>,
) : BaseViewModel<BackgroundState, BackgroundEvent, BackgroundEffect>(
    BackgroundState(),
    backgroundStateHolder,
    BackgroundUseCase()
)
```

`BackgroundPresentationModule.kt`:
```kotlin
package zhaoyun.example.composedemo.story.background.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.background.domain.BackgroundState
import zhaoyun.example.composedemo.story.background.presentation.BackgroundViewModel

val backgroundPresentationModule = module {
    viewModel { (stateHolder: StateHolder<BackgroundState>) ->
        BackgroundViewModel(backgroundStateHolder = stateHolder)
    }
}
```

- [ ] **Step 7: 编译验证 `:biz:story:presentation`**

Run: `./gradlew :biz:story:presentation:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add biz/story/
git commit -m "refactor(biz-story): rename Reducer to StateHolder across story module"
```

---

## Task 6: `:biz:feed`, `:biz:home`, `:biz:login`, `:biz:todo-list` ViewModel + DI

**Files:**
- Modify: `biz/feed/presentation/src/main/kotlin/zhaoyun/example/composedemo/feed/presentation/FeedViewModel.kt`
- Modify: `biz/feed/presentation/src/main/kotlin/zhaoyun/example/composedemo/feed/presentation/di/FeedPresentationModule.kt`
- Modify: `biz/home/presentation/src/main/kotlin/zhaoyun/example/composedemo/home/presentation/HomeViewModel.kt`
- Modify: `biz/home/presentation/src/main/kotlin/zhaoyun/example/composedemo/home/presentation/di/HomePresentationModule.kt`
- Modify: `biz/login/presentation/src/main/java/zhaoyun/example/composedemo/login/presentation/LoginViewModel.kt`
- Modify: `biz/login/presentation/src/main/java/zhaoyun/example/composedemo/login/presentation/di/LoginPresentationModule.kt`
- Modify: `biz/todo-list/presentation/src/main/java/zhaoyun/example/composedemo/todo/presentation/TodoViewModel.kt`
- Modify: `biz/todo-list/presentation/src/main/java/zhaoyun/example/composedemo/todo/presentation/di/TodoPresentationModule.kt`

- [ ] **Step 1: `FeedViewModel.kt` 与 `FeedPresentationModule.kt`**

`FeedViewModel.kt`:
```kotlin
package zhaoyun.example.composedemo.feed.presentation

import zhaoyun.example.composedemo.feed.domain.FeedEffect
import zhaoyun.example.composedemo.feed.domain.FeedEvent
import zhaoyun.example.composedemo.feed.domain.FeedState
import zhaoyun.example.composedemo.feed.domain.FeedUseCase
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder

class FeedViewModel(
    useCase: FeedUseCase,
    injectedStateHolder: StateHolder<FeedState>? = null
) : BaseViewModel<FeedState, FeedEvent, FeedEffect>(
    initialState = FeedState(),
    injectedStateHolder,
    useCase
)
```

`FeedPresentationModule.kt`:
```kotlin
package zhaoyun.example.composedemo.feed.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.feed.domain.feedDomainModule
import zhaoyun.example.composedemo.feed.domain.FeedState
import zhaoyun.example.composedemo.feed.presentation.FeedViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder

val feedPresentationModule = module {
    viewModel { params ->
        FeedViewModel(
            get(),
            params.getOrNull<StateHolder<FeedState>>()
        )
    }
}

val feedModules = listOf(
    feedDomainModule,
    feedPresentationModule
)
```

- [ ] **Step 2: `HomeViewModel.kt` 与 `HomePresentationModule.kt`**

`HomeViewModel.kt`:
```kotlin
package zhaoyun.example.composedemo.home.presentation

import zhaoyun.example.composedemo.home.domain.HomeEffect
import zhaoyun.example.composedemo.home.domain.HomeEvent
import zhaoyun.example.composedemo.home.domain.HomeState
import zhaoyun.example.composedemo.home.domain.HomeUseCase
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder

class HomeViewModel(
    useCase: HomeUseCase,
    injectedStateHolder: StateHolder<HomeState>? = null
) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(
    HomeState(),
    injectedStateHolder,
    useCase
)
```

`HomePresentationModule.kt`:
```kotlin
package zhaoyun.example.composedemo.home.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.home.domain.HomeState
import zhaoyun.example.composedemo.home.domain.homeDomainModule
import zhaoyun.example.composedemo.home.presentation.HomeViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder

val homePresentationModule = module {
    viewModel { params ->
        HomeViewModel(
            get(),
            params.getOrNull<StateHolder<HomeState>>()
        )
    }
}

val homeModules = listOf(
    homeDomainModule,
    homePresentationModule
)
```

- [ ] **Step 3: `LoginViewModel.kt` 与 `LoginPresentationModule.kt`**

`LoginViewModel.kt`:
```kotlin
package zhaoyun.example.composedemo.login.presentation

import zhaoyun.example.composedemo.login.domain.model.LoginEffect
import zhaoyun.example.composedemo.login.domain.model.LoginEvent
import zhaoyun.example.composedemo.login.domain.model.LoginState
import zhaoyun.example.composedemo.login.domain.usecase.LoginUseCase
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder

class LoginViewModel(
    useCase: LoginUseCase,
    injectedStateHolder: StateHolder<LoginState>? = null
) : BaseViewModel<LoginState, LoginEvent, LoginEffect>(
    initialState = LoginState(),
    injectedStateHolder,
    useCase
)
```

`LoginPresentationModule.kt`:
```kotlin
package zhaoyun.example.composedemo.login.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.login.domain.di.loginDomainModule
import zhaoyun.example.composedemo.login.domain.model.LoginState
import zhaoyun.example.composedemo.login.presentation.LoginViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder

/**
 * Login Presentation 层 Koin Module
 *
 * 绑定 ViewModel；依赖通过构造函数注入。
 * 支持可选注入外部 [StateHolder<LoginState>]，用于 Global 嵌入模式。
 */
val loginPresentationModule = module {
    viewModel { params ->
        LoginViewModel(
            get(),
            params.getOrNull<StateHolder<LoginState>>()
        )
    }
}

/**
 * 供 Application 层一键导入的完整 Login 模块组合。
 */
val loginModules = listOf(
    loginDomainModule,
    loginPresentationModule
)
```

- [ ] **Step 4: `TodoViewModel.kt` 与 `TodoPresentationModule.kt`**

`TodoViewModel.kt`:
```kotlin
package zhaoyun.example.composedemo.todo.presentation

import zhaoyun.example.composedemo.domain.model.TodoEffect
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.model.TodoState
import zhaoyun.example.composedemo.domain.usecase.CheckLoginUseCase
import zhaoyun.example.composedemo.domain.usecase.TodoUseCases
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder

class TodoViewModel(
    todoUseCases: TodoUseCases,
    checkLoginUseCase: CheckLoginUseCase,
    injectedStateHolder: StateHolder<TodoState>? = null
) : BaseViewModel<TodoState, TodoEvent, TodoEffect>(
    initialState = TodoState(),
    injectedStateHolder,
    todoUseCases,
    checkLoginUseCase
)
```

`TodoPresentationModule.kt`:
```kotlin
package zhaoyun.example.composedemo.todo.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.domain.di.todoDomainModule
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.domain.model.TodoState
import zhaoyun.example.composedemo.todo.presentation.TodoViewModel

/**
 * Todo List Presentation 层 Koin Module
 *
 * 绑定 ViewModel；依赖通过构造函数注入。
 * 支持可选注入外部 [StateHolder<TodoState>]，用于 Global 嵌入模式。
 * 并聚合 Domain 层的 Module。
 */
val todoPresentationModule = module {
    viewModel { params ->
        TodoViewModel(
            get(),
            get(),
            params.getOrNull<StateHolder<TodoState>>()
        )
    }
}

/**
 * 供 Application 层一键导入的完整 Todo List 模块组合。
 */
val todoModules = listOf(
    todoDomainModule,
    todoPresentationModule
)
```

- [ ] **Step 5: 编译验证所有 presentation 模块**

Run:
```bash
./gradlew :biz:feed:presentation:compileDebugKotlin :biz:home:presentation:compileDebugKotlin :biz:login:presentation:compileDebugKotlin :biz:todo-list:presentation:compileDebugKotlin
```
Expected: BUILD SUCCESSFUL for all four modules

- [ ] **Step 6: Commit**

```bash
git add biz/feed/ biz/home/ biz/login/ biz/todo-list/
git commit -m "refactor(biz): rename Reducer to StateHolder in feed/home/login/todo modules"
```

---

## Task 7: 业务模块测试

**Files:**
- Modify: `biz/todo-list/presentation/src/test/java/zhaoyun/example/composedemo/todo/presentation/TodoViewModelDelegateTest.kt`
- Modify: `app/src/androidTest/java/zhaoyun/example/composedemo/GlobalTodoEmbedAndroidTest.kt`

- [ ] **Step 1: `TodoViewModelDelegateTest.kt`**

```kotlin
package zhaoyun.example.composedemo.todo.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.usecase.CheckLoginUseCase
import zhaoyun.example.composedemo.domain.usecase.TodoUseCases
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.DelegateStateHolder
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository

/**
 * TodoViewModel注入DelegateStateHolder后的集成测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelDelegateTest {

    private val fakeRepository = FakeUserRepository()

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createDelegateViewModel(): Pair<TodoViewModel, MutableStateFlow<GlobalTestState>> {
        val globalState = MutableStateFlow(
            GlobalTestState(
                todo = zhaoyun.example.composedemo.domain.model.TodoState()
            )
        )
        val detailState = MutableStateFlow(globalState.value.todo)

        val stateHolder = DelegateStateHolder(
            state = detailState,
            onUpdate = { transform ->
                val newTodo = transform(globalState.value.todo)
                globalState.value = globalState.value.copy(todo = newTodo)
                detailState.value = newTodo
            }
        )

        val viewModel = TodoViewModel(
            todoUseCases = TodoUseCases(),
            checkLoginUseCase = CheckLoginUseCase(fakeRepository),
            injectedStateHolder = stateHolder
        )
        return viewModel to globalState
    }

    @Test
    fun `注入DelegateStateHolder后Event经代理更新外部状态`() {
        val (viewModel, globalState) = createDelegateViewModel()

        assertEquals(globalState.value.todo, viewModel.state.value)

        viewModel.onEvent(TodoEvent.OnInputTextChanged("DelegateTest"))
        assertEquals("DelegateTest", viewModel.state.value.inputText)
        assertEquals("DelegateTest", globalState.value.todo.inputText)

        viewModel.onEvent(TodoEvent.OnAddTodoClicked)
        assertEquals(1, viewModel.state.value.todos.size)
        assertEquals(1, globalState.value.todo.todos.size)
        assertEquals("DelegateTest", globalState.value.todo.todos[0].title)
    }

    @Test
    fun `StateTransform注入能正确影响Detail看到的State`() {
        val globalState = MutableStateFlow(
            GlobalTestState(
                todo = zhaoyun.example.composedemo.domain.model.TodoState()
            )
        )
        val detailState = MutableStateFlow(
            globalState.value.todo.copy(inputText = "[transformed]${globalState.value.todo.inputText}")
        )

        val stateHolder = DelegateStateHolder(
            state = detailState,
            onUpdate = { transform ->
                val newTodo = transform(globalState.value.todo)
                globalState.value = globalState.value.copy(todo = newTodo)
                detailState.value = newTodo.copy(inputText = "[transformed]${newTodo.inputText}")
            }
        )

        val viewModel = TodoViewModel(
            todoUseCases = TodoUseCases(),
            checkLoginUseCase = CheckLoginUseCase(fakeRepository),
            injectedStateHolder = stateHolder
        )

        viewModel.onEvent(TodoEvent.OnInputTextChanged("hello"))
        assertEquals("[transformed]hello", viewModel.state.value.inputText)
        assertEquals("hello", globalState.value.todo.inputText)
    }

    @Test
    fun `Delegate模式下effect仍然独立发射`() = runTest {
        val (viewModel, _) = createDelegateViewModel()

        val effects = mutableListOf<BaseEffect>()
        val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
        scope.launch { viewModel.baseEffect.collect { effects.add(it) } }

        viewModel.state.value

        viewModel.onEvent(TodoEvent.OnInputTextChanged("EffectTest"))
        viewModel.onEvent(TodoEvent.OnAddTodoClicked)

        assertTrue(effects.isNotEmpty())
        assertEquals(BaseEffect.ShowToast("添加成功"), effects[0])
    }

    data class GlobalTestState(
        val todo: zhaoyun.example.composedemo.domain.model.TodoState
    )
}
```

- [ ] **Step 2: `GlobalTodoEmbedAndroidTest.kt`**

```kotlin
package zhaoyun.example.composedemo

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import zhaoyun.example.composedemo.domain.model.TodoEvent
import zhaoyun.example.composedemo.domain.usecase.CheckLoginUseCase
import zhaoyun.example.composedemo.domain.usecase.TodoUseCases
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.service.usercenter.api.model.UserInfo
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository
import zhaoyun.example.composedemo.todo.presentation.TodoListPage
import zhaoyun.example.composedemo.todo.presentation.TodoViewModel

/**
 * Global页面嵌入Todo的集成测试
 */
@RunWith(AndroidJUnit4::class)
class GlobalTodoEmbedAndroidTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeRepository = FakeUserRepository()

    data class GlobalState(val todoCount: Int = 0)

    class GlobalViewModel : ViewModel() {
        private val _state = MutableStateFlow(GlobalState())
        val state: StateFlow<GlobalState> = _state.asStateFlow()

        fun createTodoStateHolder(): zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder<zhaoyun.example.composedemo.domain.model.TodoState> {
            val todoStateFlow = MutableStateFlow(zhaoyun.example.composedemo.domain.model.TodoState())
            return zhaoyun.example.composedemo.scaffold.core.mvi.DelegateStateHolder(
                state = todoStateFlow,
                onUpdate = { transform ->
                    val newTodo = transform(todoStateFlow.value)
                    todoStateFlow.value = newTodo
                    _state.update { it.copy(todoCount = newTodo.todos.size) }
                }
            )
        }
    }

    @Composable
    fun GlobalWithTodoScreen(globalVm: GlobalViewModel, todoVm: TodoViewModel) {
        val globalState by globalVm.state.collectAsStateWithLifecycle()
        val todoState by todoVm.state.collectAsStateWithLifecycle()

        Column {
            Text(text = "Total: ${globalState.todoCount}")
            TodoListPage(state = todoState, onEvent = todoVm::onEvent)
        }
    }

    @Test
    fun `Global嵌入Todo后添加操作同步更新全局计数`() {
        fakeRepository.setLoggedInUser(UserInfo("u_1", "alice", "Alice"))

        val globalVm = GlobalViewModel()
        val stateHolder = globalVm.createTodoStateHolder()
        val todoVm = TodoViewModel(
            todoUseCases = TodoUseCases(),
            checkLoginUseCase = CheckLoginUseCase(fakeRepository),
            injectedStateHolder = stateHolder
        )

        composeTestRule.setContent {
            GlobalWithTodoScreen(globalVm = globalVm, todoVm = todoVm)
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Total: 0").assertIsDisplayed()

        composeTestRule.onNodeWithTag("todo_input").performTextInput("EmbeddedTodo")
        composeTestRule.onNodeWithTag("add_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("EmbeddedTodo").assertIsDisplayed()
        composeTestRule.onNodeWithText("Total: 1").assertIsDisplayed()
    }
}
```

- [ ] **Step 3: 运行测试**

Run: `./gradlew :biz:todo-list:presentation:test`
Expected: BUILD SUCCESSFUL

Run: `./gradlew :app:compileDebugAndroidTest`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add biz/todo-list/presentation/src/test/ app/src/androidTest/
git commit -m "test(biz/app): adapt tests to StateHolder naming"
```

---

## Task 8: 文档更新

**Files:**
- Modify: `MVI_FRAMEWORK.md`
- Modify: `docs/superpowers/specs/2026-04-24-feed-home-design.md`
- Modify: `docs/superpowers/plans/2026-04-24-feed-home-plan.md`

- [ ] **Step 1: 更新 `MVI_FRAMEWORK.md`**

全局文本替换（保持 Markdown 结构不变）：
- `Reducer` → `StateHolder`
- `LocalReducer` → `LocalStateHolder`
- `DelegateReducer` → `DelegateStateHolder`
- `reduce(` → `update(`
- `createDelegateReducer` → `createDelegateStateHolder`
- `onReduce` → `onUpdate`
- `reducer`（泛指变量） → `stateHolder`
- `injectedReducer` → `injectedStateHolder`
- `messageReducer` / `infoBarReducer` / `inputReducer` / `backgroundReducer` → 对应 `*StateHolder`
- 注释中的「Reducer」→「StateHolder」

关键代码块示例（确认替换后）：

```kotlin
interface StateHolder<S> {
    val state: StateFlow<S>
    fun update(transform: (S) -> S)
}
```

```kotlin
class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S,
    injectedStateHolder: StateHolder<S>? = null,
    private vararg val useCases: BaseUseCase<S, E, F>
) : ViewModel() {
    private val stateHolder: StateHolder<S> = injectedStateHolder ?: LocalStateHolder(initialState)
    // ...
    protected fun updateState(transform: (S) -> S) { stateHolder.update(transform) }
    fun <T> createDelegateStateHolder(...) : StateHolder<T> { ... }
}
```

- [ ] **Step 2: 更新 `docs/superpowers/specs/2026-04-24-feed-home-design.md` 与 `docs/superpowers/plans/2026-04-24-feed-home-plan.md`**

对其中所有出现的 `Reducer`、`reduce()`、`reducer`、`LocalReducer`、`DelegateReducer` 执行同样的全局替换。

- [ ] **Step 3: Commit**

```bash
git add MVI_FRAMEWORK.md docs/superpowers/
git commit -m "docs: rename Reducer to StateHolder in all documentation"
```

---

## Task 9: 全局编译与测试验证

- [ ] **Step 1: 全量编译**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL（所有模块）

- [ ] **Step 2: 全量单元测试**

Run: `./gradlew test`
Expected: BUILD SUCCESSFUL，所有测试通过

- [ ] **Step 3: 最终搜索确认无残留**

Run:
```bash
grep -r "Reducer\|reduce(" --include="*.kt" --include="*.md" . \
  | grep -v ".git/" | grep -v "build/" | grep -v ".gradle/"
```
Expected: 无输出（或仅出现在历史提交信息中，不纳入代码）

注意：搜索应排除 `kotlinx.coroutines.flow.update` 的合法调用（即 `flow.update { }` 中的 `update` 是正确存在的，不应被误报）。

更精确的验证命令：
```bash
grep -rE "\b(Reducer|LocalReducer|DelegateReducer|createDelegateReducer|onReduce)\b" --include="*.kt" --include="*.md" . | grep -v ".git/" | grep -v "build/" | grep -v ".gradle/"
```
Expected: 无输出

- [ ] **Step 4: Commit（如有任何额外修复）**

```bash
git add -A
git commit -m "refactor: complete Reducer to StateHolder global renaming"
```

---

## Self-Review Checklist

- [ ] **Spec coverage:** 设计文档中所有命名映射项（Reducer/LocalReducer/DelegateReducer/reduce/reducer/injectedReducer/onReduce/createDelegateReducer）均有对应任务。
- [ ] **Placeholder scan:** 无 TBD/TODO/"implement later"。
- [ ] **Type consistency:** Task 1 定义的 `StateHolder<S>` / `update()` / `LocalStateHolder` / `DelegateStateHolder` 与后续所有任务中的使用完全一致。
- [ ] **File paths:** 所有路径均基于实际项目结构，无假设路径。
