# Story Input 输入框组件实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 StoryCard 底部对话输入框，支持键盘展开/收起、括号快捷插入、按钮动态切换，键盘收起通过 Koin SPI (`InputKeyboardCoordinator`) 在 FeedScreen 层统一管理。

**Architecture:** Domain 层通过 `findServiceOrNull<InputKeyboardCoordinator>()` 经 ServiceRegistry → Koin global 兜底链获取协调器；`MviScreen` 增加 `KoinServiceRegistry` 作为 `MutableServiceRegistryImpl` 的最终 parent，实现该兜底能力；Presentation 层用 `BasicTextField` + 两个 `LaunchedEffect` 分别处理 Effect 光标同步与 isFocused 键盘驱动。

**Tech Stack:** Kotlin, Jetpack Compose, MVI framework (scaffold:core/android), Koin, JUnit 4, kotlinx-coroutines-test

---

## 文件清单

| 操作 | 路径                                                                               |
|----|----------------------------------------------------------------------------------|
| 新建 | `scaffold/android/src/main/kotlin/.../KoinServiceRegistry.kt`                    |
| 修改 | `scaffold/android/src/main/kotlin/.../MviScreen.kt`                              |
| 新建 | `biz/story/input/domain/src/main/kotlin/.../InputFocusSpi.kt`                    |
| 新建 | `biz/story/input/domain/src/main/kotlin/.../InputKeyboardCoordinator.kt`         |
| 新建 | `biz/story/input/domain/src/test/kotlin/.../InputKeyboardCoordinatorTest.kt`     |
| 修改 | `biz/story/input/domain/src/main/kotlin/.../InputState.kt`                       |
| 修改 | `biz/story/input/domain/src/main/kotlin/.../InputEvent.kt`                       |
| 修改 | `biz/story/input/domain/src/main/kotlin/.../InputEffect.kt`                      |
| 新建 | `biz/story/input/domain/src/test/kotlin/.../InputUseCaseTest.kt`                 |
| 修改 | `biz/story/input/domain/src/main/kotlin/.../InputUseCase.kt`                     |
| 修改 | `biz/story/input/presentation/src/main/kotlin/.../InputViewModel.kt`             |
| 修改 | `biz/story/input/presentation/src/main/kotlin/.../di/InputPresentationModule.kt` |
| 修改 | `biz/story/input/presentation/build.gradle.kts`                                  |
| 修改 | `biz/story/input/presentation/src/main/kotlin/.../InputArea.kt`                  |
| 修改 | `biz/feed/presentation/src/main/kotlin/.../FeedScreen.kt`                        |
| 修改 | `app/src/main/java/zhaoyun/example/composedemo/ComposeDemoApp.kt`                |

> 路径中 `...` = `zhaoyun/example/composedemo/`，下同。

---

## Task 1: KoinServiceRegistry + MviScreen（Scaffold 层扩展）

**Files:**

- 新建: `scaffold/android/src/main/kotlin/zhaoyun/example/composedemo/scaffold/android/KoinServiceRegistry.kt`
- 修改: `scaffold/android/src/main/kotlin/zhaoyun/example/composedemo/scaffold/android/MviScreen.kt`

- [ ] **Step 1: 新建 KoinServiceRegistry.kt**

```kotlin
package zhaoyun.example.composedemo.scaffold.android

import org.koin.core.Koin
import org.koin.core.qualifier.named
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry

class KoinServiceRegistry(private val koin: Koin) : ServiceRegistry {
    override fun <T : Any> find(clazz: Class<T>, tag: String?): T? =
        koin.getOrNull(clazz.kotlin, tag?.let { named(it) })
}
```

- [ ] **Step 2: 修改 MviScreen.kt — 为 screenRegistry 加 Koin global 兜底**

找到以下这一行：

```kotlin
val screenRegistry = remember { MutableServiceRegistryImpl(logger = AndroidMviLogger) }
```

替换为：

```kotlin
val koinRegistry = remember { KoinServiceRegistry(koin) }
val screenRegistry = remember { MutableServiceRegistryImpl(parent = koinRegistry, logger = AndroidMviLogger) }
```

`koin` 变量已在 `MviScreen` 函数体开头通过 `val koin = getKoin()` 声明，无需额外修改。

- [ ] **Step 3: 验证编译**

```bash
./gradlew :scaffold:android:assembleDebug
```

期望：BUILD SUCCESSFUL，无编译错误。

- [ ] **Step 4: Commit**

```bash
git add scaffold/android/src/main/kotlin/zhaoyun/example/composedemo/scaffold/android/KoinServiceRegistry.kt \
        scaffold/android/src/main/kotlin/zhaoyun/example/composedemo/scaffold/android/MviScreen.kt
git commit -m "feat(scaffold): add KoinServiceRegistry for Koin global fallback in ServiceRegistry lookup chain"
```

---

## Task 2: InputFocusSpi + InputKeyboardCoordinator（TDD）

**Files:**

- 新建: `biz/story/input/domain/src/main/kotlin/zhaoyun/example/composedemo/story/input/domain/InputFocusSpi.kt`
- 新建: `biz/story/input/domain/src/main/kotlin/zhaoyun/example/composedemo/story/input/domain/InputKeyboardCoordinator.kt`
- 新建: `biz/story/input/domain/src/test/kotlin/zhaoyun/example/composedemo/story/input/domain/InputKeyboardCoordinatorTest.kt`

- [ ] **Step 1: 新建测试目录结构**

```bash
mkdir -p biz/story/input/domain/src/test/kotlin/zhaoyun/example/composedemo/story/input/domain
```

- [ ] **Step 2: 写失败测试 InputKeyboardCoordinatorTest.kt**

```kotlin
package zhaoyun.example.composedemo.story.input.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class InputKeyboardCoordinatorTest {

    @Test
    fun `test_coordinator_注册单个SPI后requestDismiss调用一次dismissKeyboard`() {
        val coordinator = InputKeyboardCoordinator()
        var callCount = 0
        val spi = object : InputFocusSpi {
            override fun dismissKeyboard() { callCount++ }
        }
        coordinator.register(spi)
        coordinator.requestDismiss()
        assertEquals(1, callCount)
    }

    @Test
    fun `test_coordinator_注册多个SPI后requestDismiss全部调用`() {
        val coordinator = InputKeyboardCoordinator()
        var callCount = 0
        val spi1 = object : InputFocusSpi { override fun dismissKeyboard() { callCount++ } }
        val spi2 = object : InputFocusSpi { override fun dismissKeyboard() { callCount++ } }
        coordinator.register(spi1)
        coordinator.register(spi2)
        coordinator.requestDismiss()
        assertEquals(2, callCount)
    }

    @Test
    fun `test_coordinator_未注册任何SPI时requestDismiss不崩溃`() {
        val coordinator = InputKeyboardCoordinator()
        coordinator.requestDismiss() // should not throw
    }
}
```

- [ ] **Step 3: 运行测试，确认因类不存在而失败**

```bash
./gradlew :biz:story:input:domain:test
```

期望：编译失败（`InputFocusSpi` 和 `InputKeyboardCoordinator` 未定义）。

- [ ] **Step 4: 新建 InputFocusSpi.kt**

```kotlin
package zhaoyun.example.composedemo.story.input.domain

interface InputFocusSpi {
    fun dismissKeyboard()
}
```

- [ ] **Step 5: 新建 InputKeyboardCoordinator.kt**

```kotlin
package zhaoyun.example.composedemo.story.input.domain

class InputKeyboardCoordinator {
    private val listeners = java.util.WeakHashMap<InputFocusSpi, Unit>()

    @Synchronized
    fun register(spi: InputFocusSpi) {
        listeners[spi] = Unit
    }

    @Synchronized
    fun requestDismiss() {
        listeners.keys.toList().forEach { it.dismissKeyboard() }
    }
}
```

- [ ] **Step 6: 运行测试，确认通过**

```bash
./gradlew :biz:story:input:domain:test
```

期望：3 个测试全部 PASS。

- [ ] **Step 7: Commit**

```bash
git add biz/story/input/domain/src/main/kotlin/zhaoyun/example/composedemo/story/input/domain/InputFocusSpi.kt \
        biz/story/input/domain/src/main/kotlin/zhaoyun/example/composedemo/story/input/domain/InputKeyboardCoordinator.kt \
        biz/story/input/domain/src/test/kotlin/zhaoyun/example/composedemo/story/input/domain/InputKeyboardCoordinatorTest.kt
git commit -m "feat(input:domain): add InputFocusSpi and InputKeyboardCoordinator with WeakHashMap listener management"
```

---

## Task 3: 重写 Domain 数据模型（State / Event / Effect）

**Files:**

- 修改: `biz/story/input/domain/src/main/kotlin/zhaoyun/example/composedemo/story/input/domain/InputState.kt`
- 修改: `biz/story/input/domain/src/main/kotlin/zhaoyun/example/composedemo/story/input/domain/InputEvent.kt`
- 修改: `biz/story/input/domain/src/main/kotlin/zhaoyun/example/composedemo/story/input/domain/InputEffect.kt`

- [ ] **Step 1: 重写 InputState.kt**

完整替换文件内容：

```kotlin
package zhaoyun.example.composedemo.story.input.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class InputState(
    val text: String = "",
    val isFocused: Boolean = false,
    val hintText: String = "自由输入...",
) : UiState
```

- [ ] **Step 2: 重写 InputEvent.kt**

完整替换文件内容：

```kotlin
package zhaoyun.example.composedemo.story.input.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class InputEvent : UiEvent {
    data class OnTextChanged(val text: String) : InputEvent()
    data class OnFocusChanged(val focused: Boolean) : InputEvent()
    object OnBracketClicked : InputEvent()
    object OnVoiceClicked : InputEvent()
    object OnPlusClicked : InputEvent()
    object OnSendClicked : InputEvent()
}
```

- [ ] **Step 3: 重写 InputEffect.kt**

完整替换文件内容：

```kotlin
package zhaoyun.example.composedemo.story.input.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect

sealed class InputEffect : UiEffect {
    data class InsertBrackets(val newText: String, val cursorPosition: Int) : InputEffect()
}
```

- [ ] **Step 4: 验证编译**

```bash
./gradlew :biz:story:input:domain:assembleDebug
```

期望：BUILD SUCCESSFUL（此时 InputUseCase 可能报错，因为旧 Event 分支不再存在，下一个 Task 修复）。

- [ ] **Step 5: Commit**

```bash
git add biz/story/input/domain/src/main/kotlin/zhaoyun/example/composedemo/story/input/domain/InputState.kt \
        biz/story/input/domain/src/main/kotlin/zhaoyun/example/composedemo/story/input/domain/InputEvent.kt \
        biz/story/input/domain/src/main/kotlin/zhaoyun/example/composedemo/story/input/domain/InputEffect.kt
git commit -m "feat(input:domain): rewrite State/Event/Effect to match UC-01~09"
```

---

## Task 4: InputUseCase TDD（核心业务逻辑）

**Files:**

- 新建: `biz/story/input/domain/src/test/kotlin/zhaoyun/example/composedemo/story/input/domain/InputUseCaseTest.kt`
- 修改: `biz/story/input/domain/src/main/kotlin/zhaoyun/example/composedemo/story/input/domain/InputUseCase.kt`

- [ ] **Step 1: 写失败测试 InputUseCaseTest.kt**

```kotlin
package zhaoyun.example.composedemo.story.input.domain

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

class InputUseCaseTest {

    private lateinit var coordinator: InputKeyboardCoordinator
    private lateinit var useCase: InputUseCase

    @Before
    fun setup() {
        coordinator = InputKeyboardCoordinator()
        val registry = MutableServiceRegistryImpl()
        // 手动注册 coordinator，模拟 Koin global 兜底的效果
        registry.register(InputKeyboardCoordinator::class.java, coordinator)
        useCase = InputUseCase(InputState().toStateHolder(), registry)
    }

    // UC-01
    @Test
    fun `test_UC01_点击后isFocused变为true`() = runTest {
        useCase.receiveEvent(InputEvent.OnFocusChanged(true))
        assertTrue(useCase.state.value.isFocused)
    }

    // UC-02
    @Test
    fun `test_UC02_coordinator调用后isFocused变为false`() = runTest {
        useCase.receiveEvent(InputEvent.OnFocusChanged(true))
        coordinator.requestDismiss()
        assertFalse(useCase.state.value.isFocused)
    }

    // UC-02
    @Test
    fun `test_UC02_收起后文字内容保留`() = runTest {
        useCase.receiveEvent(InputEvent.OnTextChanged("hello"))
        useCase.receiveEvent(InputEvent.OnFocusChanged(true))
        coordinator.requestDismiss()
        assertEquals("hello", useCase.state.value.text)
        assertFalse(useCase.state.value.isFocused)
    }

    // UC-06
    @Test
    fun `test_UC06_点击括号在末尾追加全角括号`() = runTest {
        useCase.receiveEvent(InputEvent.OnTextChanged("你好"))
        useCase.receiveEvent(InputEvent.OnBracketClicked)
        assertEquals("你好（）", useCase.state.value.text)
    }

    // UC-06
    @Test
    fun `test_UC06_光标位置在括号中间`() = runTest {
        useCase.receiveEvent(InputEvent.OnTextChanged("你好"))

        val effects = mutableListOf<InputEffect>()
        val collectJob = launch { useCase.effect.collect { effects += it } }

        useCase.receiveEvent(InputEvent.OnBracketClicked)
        advanceUntilIdle()
        collectJob.cancel()

        val effect = effects.filterIsInstance<InputEffect.InsertBrackets>().first()
        assertEquals("你好（）", effect.newText)
        // cursorPosition 指向 ）之前，即 length - 1
        assertEquals("你好（）".length - 1, effect.cursorPosition)
    }

    // UC-09
    @Test
    fun `test_UC09_有文字时text非空`() = runTest {
        useCase.receiveEvent(InputEvent.OnTextChanged("x"))
        assertTrue(useCase.state.value.text.isNotEmpty())
    }

    // UC-09
    @Test
    fun `test_UC09_清空文字后text为空`() = runTest {
        useCase.receiveEvent(InputEvent.OnTextChanged("x"))
        useCase.receiveEvent(InputEvent.OnTextChanged(""))
        assertTrue(useCase.state.value.text.isEmpty())
    }

    // UC-07
    @Test
    fun `test_UC07_语音点击无状态副作用`() = runTest {
        val stateBefore = useCase.state.value
        useCase.receiveEvent(InputEvent.OnVoiceClicked)
        assertEquals(stateBefore, useCase.state.value)
    }

    // UC-08
    @Test
    fun `test_UC08_加号点击无状态副作用`() = runTest {
        val stateBefore = useCase.state.value
        useCase.receiveEvent(InputEvent.OnPlusClicked)
        assertEquals(stateBefore, useCase.state.value)
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

```bash
./gradlew :biz:story:input:domain:test
```

期望：编译失败或测试失败（InputUseCase 还未更新）。

- [ ] **Step 3: 重写 InputUseCase.kt**

完整替换文件内容：

```kotlin
package zhaoyun.example.composedemo.story.input.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.findServiceOrNull
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class InputUseCase(
    stateHolder: StateHolder<InputState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<InputState, InputEvent, InputEffect>(stateHolder, serviceRegistry),
    InputFocusSpi {

    init {
        // ServiceRegistry → Koin global 兜底链；测试中直接注入 coordinator
        findServiceOrNull<InputKeyboardCoordinator>()?.register(this)
    }

    // UC-02：coordinator 广播时调用，仅修改 isFocused，保留 text
    override fun dismissKeyboard() {
        updateState { it.copy(isFocused = false) }
    }

    override suspend fun onEvent(event: InputEvent) {
        when (event) {
            is InputEvent.OnTextChanged ->
                updateState { it.copy(text = event.text) }

            is InputEvent.OnFocusChanged ->
                updateState { it.copy(isFocused = event.focused) }

            is InputEvent.OnBracketClicked -> {
                val newText = currentState.text + "（）"
                updateState { it.copy(text = newText) }
                dispatchEffect(InputEffect.InsertBrackets(newText, newText.length - 1))
            }

            is InputEvent.OnVoiceClicked -> Unit
            is InputEvent.OnPlusClicked -> Unit
            is InputEvent.OnSendClicked -> Unit
        }
    }
}
```

- [ ] **Step 4: 运行测试，确认全部通过**

```bash
./gradlew :biz:story:input:domain:test
```

期望：9 个测试（含 Task 2 的 3 个）全部 PASS。

- [ ] **Step 5: Commit**

```bash
git add biz/story/input/domain/src/test/kotlin/zhaoyun/example/composedemo/story/input/domain/InputUseCaseTest.kt \
        biz/story/input/domain/src/main/kotlin/zhaoyun/example/composedemo/story/input/domain/InputUseCase.kt
git commit -m "feat(input:domain): implement InputUseCase with TDD — covers UC-01/02/06/07/08/09"
```

---

## Task 5: Presentation 层接线（ViewModel / Module / build.gradle）

**Files:**

- 修改: `biz/story/input/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/input/presentation/InputViewModel.kt`
- 修改: `biz/story/input/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/input/presentation/di/InputPresentationModule.kt`
- 修改: `biz/story/input/presentation/build.gradle.kts`

- [ ] **Step 1: 重写 InputViewModel.kt**

完整替换文件内容：

```kotlin
package zhaoyun.example.composedemo.story.input.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.input.domain.InputEffect
import zhaoyun.example.composedemo.story.input.domain.InputEvent
import zhaoyun.example.composedemo.story.input.domain.InputState
import zhaoyun.example.composedemo.story.input.domain.InputUseCase

class InputViewModel(
    stateHolder: StateHolder<InputState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<InputState, InputEvent, InputEffect>(
    stateHolder,
    serviceRegistry,
    { holder, registry -> InputUseCase(holder, registry) },
)
```

- [ ] **Step 2: 重写 InputPresentationModule.kt**

完整替换文件内容：

```kotlin
package zhaoyun.example.composedemo.story.input.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.android.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.input.domain.InputState
import zhaoyun.example.composedemo.story.input.presentation.InputViewModel

val inputPresentationModule = module {
    scope(MviKoinScopes.Item) {
        viewModel { (stateHolder: StateHolder<InputState>) ->
            InputViewModel(stateHolder, get())
        }
    }
}
```

- [ ] **Step 3: 修改 build.gradle.kts，添加 material-icons-extended**

在 `dependencies { ... }` 块中追加：

```kotlin
implementation("androidx.compose.material:material-icons-extended")
```

- [ ] **Step 4: 验证编译**

```bash
./gradlew :biz:story:input:presentation:assembleDebug
```

期望：BUILD SUCCESSFUL。

- [ ] **Step 5: Commit**

```bash
git add biz/story/input/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/input/presentation/InputViewModel.kt \
        biz/story/input/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/input/presentation/di/InputPresentationModule.kt \
        biz/story/input/presentation/build.gradle.kts
git commit -m "feat(input:presentation): update InputViewModel and DI module, add material-icons-extended"
```

---

## Task 6: 重写 InputArea（UC-03/04/05 UI 实现）

**Files:**

- 修改: `biz/story/input/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/input/presentation/InputArea.kt`

- [ ] **Step 1: 完整替换 InputArea.kt**

```kotlin
package zhaoyun.example.composedemo.story.input.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import zhaoyun.example.composedemo.story.input.domain.InputEffect
import zhaoyun.example.composedemo.story.input.domain.InputEvent

@Composable
fun InputArea(
    viewModel: InputViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue()) }

    // UC-06：收集 InsertBrackets effect，同步含光标位置的 TextFieldValue
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is InputEffect.InsertBrackets ->
                    textFieldValue = TextFieldValue(
                        text = effect.newText,
                        selection = TextRange(effect.cursorPosition),
                    )
            }
        }
    }

    // UC-01/02：isFocused 状态驱动键盘显隐
    // 注意：isFocused=false 时只隐藏键盘，不 clearFocus()，避免与 onFocusChanged 互相触发循环
    LaunchedEffect(state.isFocused) {
        if (state.isFocused) {
            focusRequester.requestFocus()
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
            )
            // 消费来自 Row 背景的 tap，防止 FeedScreen.detectTapGestures 误触发 dismiss
            // 同时实现「点击输入框任意区域打开键盘」的 UX（UC-01）
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { focusRequester.requestFocus() },
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // UC-03：单行，hint 超出以省略号截断
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                viewModel.sendEvent(InputEvent.OnTextChanged(newValue.text))
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    viewModel.sendEvent(InputEvent.OnFocusChanged(focusState.isFocused))
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            textStyle = LocalTextStyle.current.copy(
                color = Color.White,
                fontSize = 14.sp,
            ),
            cursorBrush = SolidColor(Color.White),
            decorationBox = { innerTextField ->
                if (textFieldValue.text.isEmpty()) {
                    Text(
                        text = state.hintText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                innerTextField()
            },
        )

        // UC-05：展开状态（isFocused）额外显示括号按钮
        AnimatedVisibility(visible = state.isFocused) {
            IconButton(onClick = { viewModel.sendEvent(InputEvent.OnBracketClicked) }) {
                Text(
                    text = "( )",
                    color = Color.White,
                    fontSize = 14.sp,
                )
            }
        }

        // UC-04/07：语音按钮，始终显示（占位）
        IconButton(onClick = { viewModel.sendEvent(InputEvent.OnVoiceClicked) }) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "语音输入",
                tint = Color.White,
            )
        }

        // UC-04/08/09：有文字时显示发送，无文字时显示加号
        if (state.text.isNotEmpty()) {
            IconButton(onClick = { viewModel.sendEvent(InputEvent.OnSendClicked) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    tint = Color.White,
                )
            }
        } else {
            IconButton(onClick = { viewModel.sendEvent(InputEvent.OnPlusClicked) }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "更多",
                    tint = Color.White,
                )
            }
        }
    }
}
```

- [ ] **Step 2: 验证编译**

```bash
./gradlew :biz:story:input:presentation:assembleDebug
```

期望：BUILD SUCCESSFUL，无未解析的 import。

- [ ] **Step 3: Commit**

```bash
git add biz/story/input/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/input/presentation/InputArea.kt
git commit -m "feat(input:presentation): rewrite InputArea with BasicTextField, animated bracket button, dynamic send/plus toggle"
```

---

## Task 7: App 注册 + FeedScreen 集成

**Files:**

- 修改: `app/src/main/java/zhaoyun/example/composedemo/ComposeDemoApp.kt`
- 修改: `biz/feed/presentation/src/main/kotlin/zhaoyun/example/composedemo/feed/presentation/FeedScreen.kt`

- [ ] **Step 1: 修改 ComposeDemoApp.kt — 注册 InputKeyboardCoordinator 单例**

在 `startKoin { modules(...) }` 的末尾列表中追加一个模块。将现有代码：

```kotlin
listOf(
    module {
        single { FakeFeedRepository() } bind FeedRepository::class
    },
)
```

改为：

```kotlin
listOf(
    module {
        single { FakeFeedRepository() } bind FeedRepository::class
        single { zhaoyun.example.composedemo.story.input.domain.InputKeyboardCoordinator() }
    },
)
```

- [ ] **Step 2: 修改 FeedScreen.kt — 添加全局 tap 检测**

在文件顶部增加两行 import（与现有 import 块合并）：

```kotlin
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import org.koin.compose.koinInject
import zhaoyun.example.composedemo.story.input.domain.InputKeyboardCoordinator
```

将 `MviScreen<FeedViewModel> { viewModel ->` 内部的 `Box` 修改为带 tap 检测：

原始代码：

```kotlin
Box(modifier = modifier.fillMaxSize()) {
```

替换为：

```kotlin
val coordinator = koinInject<InputKeyboardCoordinator>()
Box(
    modifier = modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures { coordinator.requestDismiss() }
        }
) {
```

- [ ] **Step 3: 完整构建验证**

```bash
./gradlew assembleDebug
```

期望：BUILD SUCCESSFUL，所有模块编译通过。

- [ ] **Step 4: 运行所有单元测试**

```bash
./gradlew test
```

期望：全部测试 PASS（含 Task 2 的 coordinator 测试、Task 4 的 useCase 测试）。

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/zhaoyun/example/composedemo/ComposeDemoApp.kt \
        biz/feed/presentation/src/main/kotlin/zhaoyun/example/composedemo/feed/presentation/FeedScreen.kt
git commit -m "feat: register InputKeyboardCoordinator singleton and wire FeedScreen tap-to-dismiss keyboard via SPI"
```

---

## UC 覆盖率检查（实现完成后填写）

| UC ID | 标题          | 测试方法                                              | 实现位置                                                                | 状态 |
|-------|-------------|---------------------------------------------------|---------------------------------------------------------------------|----|
| UC-01 | 点击后键盘展开     | `test_UC01_点击后isFocused变为true`                    | `InputUseCase.onEvent(OnFocusChanged)` + `InputArea LaunchedEffect` | ⬜  |
| UC-02 | 点击外部收起键盘    | `test_UC02_coordinator调用后isFocused变为false`        | `InputUseCase.dismissKeyboard()` + `FeedScreen.detectTapGestures`   | ⬜  |
| UC-03 | 单行省略显示      | 间接：`isFocused/text` 状态正确                          | `BasicTextField(singleLine=true)` + `decorationBox` hint            | ⬜  |
| UC-04 | 收起显示两按钮     | 间接：`isFocused=false` 时括号按钮不可见                     | `AnimatedVisibility(visible=isFocused)`                             | ⬜  |
| UC-05 | 展开额外显示括号按钮  | 间接：`isFocused=true` 时括号按钮可见                       | `AnimatedVisibility(visible=isFocused)`                             | ⬜  |
| UC-06 | 括号按钮插入并定位光标 | `test_UC06_点击括号在末尾追加全角括号` + `test_UC06_光标位置在括号中间` | `InputUseCase.onEvent(OnBracketClicked)` + `InsertBrackets` effect  | ⬜  |
| UC-07 | 语音按钮占位      | `test_UC07_语音点击无状态副作用`                            | `OnVoiceClicked -> Unit`                                            | ⬜  |
| UC-08 | 加号面板占位      | `test_UC08_加号点击无状态副作用`                            | `OnPlusClicked -> Unit`                                             | ⬜  |
| UC-09 | 有文字切换发送按钮   | `test_UC09_有文字时text非空` + `test_UC09_清空文字后text为空`  | `OnTextChanged` + `if (state.text.isNotEmpty())`                    | ⬜  |
