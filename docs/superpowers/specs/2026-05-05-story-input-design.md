# Story Input 输入框组件设计文档

**日期**：2026-05-05  
**模块**：`:biz:story:input:domain` / `:biz:story:input:presentation`  
**关联需求**：`biz/story/input/domain/feature.md`（UC-01 ～ UC-09）

---

## 一、背景与目标

在 StoryCard 页面底部实现一个对话输入框，支持自由文字输入、括号快捷插入、语音占位、加号/发送动态切换。键盘的收起由 FeedScreen 级别统一管理，通过 Koin SPI 解耦 feed 与 input 两个业务模块。

---

## 二、架构总览

### 模块变更范围

| 模块 | 变更类型 | 说明 |
|------|---------|------|
| `scaffold:android` | 新增 + 修改 | 新增 `KoinServiceRegistry`；修改 `MviScreen` 使 ServiceRegistry 兜底到 Koin global |
| `biz:story:input:domain` | 新增 + 重写 | 新增 `InputKeyboardCoordinator` / `InputFocusSpi`；重写 State / Event / Effect / UseCase |
| `biz:story:input:presentation` | 重写 | 重写 `InputArea`；`InputViewModel` 移除 coordinator 构造参数 |
| `biz:feed:presentation` | 小改 | `FeedScreen` 增加全局 tap 手势检测 |
| `app` | 小改 | 注册 `InputKeyboardCoordinator` 为 Koin `single {}` |

### 键盘收起信号流

```
用户 tap StoryCard 背景（任意非 InputArea 区域）
    ↓
FeedScreen.pointerInput.detectTapGestures
    ↓  koinInject<InputKeyboardCoordinator>()
InputKeyboardCoordinator.requestDismiss()
    ↓  WeakHashMap 回调
InputUseCase.dismissKeyboard()
    ↓
updateState { isFocused = false }
    ↓
InputArea LaunchedEffect(state.isFocused) → keyboardController.hide()
```

### ServiceRegistry → Koin Global 兜底链

```
UseCase.findServiceOrNull<T>()
    → 当前 MviItemScope registry（own services）
    → 父 MviScreen registry（FeedScreen 级别）
    → KoinServiceRegistry（Koin global singleton）
```

---

## 三、Scaffold 层变更

### 新增：`KoinServiceRegistry.kt`（`scaffold:android`）

`ServiceRegistry` 接口的 Koin 适配器，作为 `MviScreen` 创建的 `MutableServiceRegistryImpl` 的最终 parent，实现 CLAUDE.md 中描述的「Koin global 兜底」。

```kotlin
class KoinServiceRegistry(private val koin: Koin) : ServiceRegistry {
    override fun <T : Any> find(clazz: Class<T>, tag: String?): T? =
        koin.getOrNull(clazz.kotlin, tag?.let { named(it) })
}
```

### 修改：`MviScreen.kt`

```kotlin
// 原：
val screenRegistry = remember { MutableServiceRegistryImpl(logger = AndroidMviLogger) }

// 改：
val koinRegistry = remember { KoinServiceRegistry(koin) }
val screenRegistry = remember { MutableServiceRegistryImpl(parent = koinRegistry, logger = AndroidMviLogger) }
```

`MviScope` / `MviItemScope` 无需改动，parent 链自动延伸。

---

## 四、Domain 层

### `InputState.kt`

```kotlin
data class InputState(
    val text: String = "",
    val isFocused: Boolean = false,
    val hintText: String = "自由输入...",
) : UiState
```

`showSendButton`（`text.isNotEmpty()`）和 `showBracketButton`（`isFocused`）在 Composable 层实时计算，不放入 State。

### `InputEvent.kt`

```kotlin
sealed class InputEvent : UiEvent {
    data class OnTextChanged(val text: String) : InputEvent()   // UC-01/09
    data class OnFocusChanged(val focused: Boolean) : InputEvent() // UC-01/02
    object OnBracketClicked : InputEvent()  // UC-06
    object OnVoiceClicked : InputEvent()    // UC-07 占位
    object OnPlusClicked : InputEvent()     // UC-08 占位
    object OnSendClicked : InputEvent()     // UC-09 占位
}
```

### `InputEffect.kt`

```kotlin
sealed class InputEffect : UiEffect {
    // UC-06：括号插入完成后，通知 UI 将 TextFieldValue 的光标移到括号中间
    data class InsertBrackets(val newText: String, val cursorPosition: Int) : InputEffect()
}
```

旧有的 `NavigateToChat` / `SendMessage` 属于未实现的未来功能，当前需求不覆盖，删除。

### `InputFocusSpi.kt` + `InputKeyboardCoordinator.kt`（新增）

```kotlin
interface InputFocusSpi {
    fun dismissKeyboard()
}

class InputKeyboardCoordinator {
    private val listeners = java.util.WeakHashMap<InputFocusSpi, Unit>()

    @Synchronized
    fun register(spi: InputFocusSpi) { listeners[spi] = Unit }

    @Synchronized
    fun requestDismiss() {
        listeners.keys.toList().forEach { it.dismissKeyboard() }
    }
}
```

**WeakHashMap 选型原因**：`BaseUseCase` 无 `onCleared()` 生命周期回调，无法主动反注册。WeakHashMap 以弱引用持有 key，当 Card 滑出视口、ViewModel 被 GC 后，对应的 `InputUseCase` 弱引用自动失效，`requestDismiss()` 下次调用时清理，无内存泄漏。

多张预加载 Card（`beyondViewportPageCount = 1`，最多 3 张）同时注册，`requestDismiss()` 统一广播，仅处于 focused 状态的那张有实际效果，其他张 `updateState { isFocused = false }` 为幂等 no-op。

### `InputUseCase.kt`

```kotlin
class InputUseCase(
    stateHolder: StateHolder<InputState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<InputState, InputEvent, InputEffect>(stateHolder, serviceRegistry),
    InputFocusSpi {

    init {
        // ServiceRegistry → Koin global 兜底，无需构造函数注入
        findServiceOrNull<InputKeyboardCoordinator>()?.register(this)
    }

    // UC-02
    override fun dismissKeyboard() {
        updateState { it.copy(isFocused = false) }
    }

    override suspend fun onEvent(event: InputEvent) {
        when (event) {
            is InputEvent.OnTextChanged ->              // UC-09
                updateState { it.copy(text = event.text) }

            is InputEvent.OnFocusChanged ->             // UC-01 / UC-02
                updateState { it.copy(isFocused = event.focused) }

            is InputEvent.OnBracketClicked -> {         // UC-06
                val newText = currentState.text + "（）"
                updateState { it.copy(text = newText) }
                dispatchEffect(InputEffect.InsertBrackets(newText, newText.length - 1))
            }

            is InputEvent.OnVoiceClicked -> Unit        // UC-07 占位
            is InputEvent.OnPlusClicked -> Unit         // UC-08 占位
            is InputEvent.OnSendClicked -> Unit         // UC-09 占位
        }
    }
}
```

### UC → 业务逻辑映射

| UC | 触发路径 | UseCase 行为 |
|----|---------|-------------|
| UC-01 | `OnFocusChanged(true)` | `isFocused = true` |
| UC-02 | coordinator → `dismissKeyboard()` | `isFocused = false`，text 保留 |
| UC-03 | — | 纯 UI，`BasicTextField(singleLine=true)` + hint 用 `TextOverflow.Ellipsis` |
| UC-04 | — | 纯 UI，收起状态渲染两按钮 |
| UC-05 | — | 纯 UI，`AnimatedVisibility` 控制括号按钮 |
| UC-06 | `OnBracketClicked` | append `（）`，emit `InsertBrackets(newText, length-1)` |
| UC-07 | `OnVoiceClicked` | no-op |
| UC-08 | `OnPlusClicked` | no-op |
| UC-09 | `OnTextChanged` | 更新 `text`，UI 据 `text.isNotEmpty()` 切换按钮 |

---

## 五、Presentation 层

### `InputViewModel.kt`

```kotlin
class InputViewModel(
    stateHolder: StateHolder<InputState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<InputState, InputEvent, InputEffect>(
    stateHolder,
    serviceRegistry,
    { holder, registry -> InputUseCase(holder, registry) },
)
```

### `InputPresentationModule.kt`

```kotlin
val inputPresentationModule = module {
    scope(MviKoinScopes.Item) {
        viewModel { (stateHolder: StateHolder<InputState>) ->
            InputViewModel(stateHolder, get())
        }
    }
}
```

### `InputArea.kt` 结构

**局部状态：**

```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
val focusRequester = remember { FocusRequester() }
val keyboardController = LocalSoftwareKeyboardController.current
var textFieldValue by remember { mutableStateOf(TextFieldValue()) }
```

**LaunchedEffect（两个）：**

```kotlin
// Effect 收集：InsertBrackets → 同步 TextFieldValue 含光标位置（UC-06）
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

// 状态驱动：isFocused 变化 → 控制键盘显隐（UC-01/02）
// 注意：isFocused=false 时只隐藏键盘，不 clearFocus()，避免与 TextField.onFocusChanged 互相触发循环
LaunchedEffect(state.isFocused) {
    if (state.isFocused) {
        focusRequester.requestFocus()
        keyboardController?.show()
    } else {
        keyboardController?.hide()
    }
}
```

**布局结构：**

```
Row（外层容器）
├── Modifier.clickable(onClick = { focusRequester.requestFocus() })
│     消费来自 Row 背景区域的 tap，防止 FeedScreen 的 detectTapGestures 误触发 dismiss
│     同时实现「点击整个输入框区域打开键盘」的 UX
│
├── BasicTextField
│     singleLine = true
│     focusRequester / onFocusChanged → OnFocusChanged
│     decorationBox 中 hint 用 Text(overflow = TextOverflow.Ellipsis)（UC-03）
│
├── AnimatedVisibility(visible = state.isFocused)
│     括号按钮 Text("( )")（UC-05/06）
│
├── 语音按钮 Icons.Default.Mic（UC-04/07）
│
└── if (state.text.isNotEmpty())（UC-09）
      发送按钮 Icons.AutoMirrored.Filled.Send
    else
      加号按钮 Icons.Default.Add（UC-04/08）
```

**tap 事件消费规则（防止误触发 FeedScreen dismiss）：**

- `BasicTextField` 内部消费 down event → FeedScreen 的 `detectTapGestures(requireUnconsumed=true)` 不触发
- 操作按钮 `Modifier.clickable` 消费 down event → 不触发
- InputArea 外层 Row 的 `Modifier.clickable` 消费来自 Row 背景的 tap → 不触发
- 只有 StoryCard 背景等无 click handler 的区域 tap → FeedScreen 触发 dismiss ✓

---

## 六、Feed 集成

### `FeedScreen.kt`（局部修改）

```kotlin
MviScreen<FeedViewModel> { viewModel ->
    val coordinator = koinInject<InputKeyboardCoordinator>()

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { coordinator.requestDismiss() }
            }
    ) {
        // 原有 pager 内容不变
    }
}
```

`FeedEvent` / `FeedUseCase` 无需改动，feed domain 对 input 模块无感知。

### `App` 模块（增加一行）

```kotlin
single { InputKeyboardCoordinator() }
```

---

## 七、测试策略

测试文件路径：`biz/story/input/domain/src/test/kotlin/.../InputUseCaseTest.kt`

| 测试方法 | 对应 UC | 验证内容 |
|---------|---------|---------|
| `test_UC01_点击后isFocused变为true` | UC-01 | `OnFocusChanged(true)` → `state.isFocused = true` |
| `test_UC02_coordinator调用后isFocused变为false` | UC-02 | `coordinator.requestDismiss()` → `state.isFocused = false` |
| `test_UC02_收起后文字内容保留` | UC-02 | dismiss 后 `state.text` 不变 |
| `test_UC06_点击括号在末尾追加全角括号` | UC-06 | `OnBracketClicked` → `text` 末尾有 `（）` |
| `test_UC06_光标位置在括号中间` | UC-06 | `InsertBrackets.cursorPosition == text.length - 1` |
| `test_UC09_有文字时text非空` | UC-09 | `OnTextChanged("x")` → `state.text.isNotEmpty() = true` |
| `test_UC09_清空文字后text为空` | UC-09 | `OnTextChanged("")` → `state.text.isEmpty() = true` |
| `test_UC07_语音点击无副作用` | UC-07 | `OnVoiceClicked` → state 不变、无 effect |
| `test_UC08_加号点击无副作用` | UC-08 | `OnPlusClicked` → state 不变、无 effect |

UC-03/04/05 为纯 UI 逻辑，通过 state 的布尔值间接验证（`isFocused` / `text.isNotEmpty()`）。
