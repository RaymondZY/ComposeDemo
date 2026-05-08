# Story InfoBar Presentation 层设计

## 目标

实现 Story InfoBar presentation 层完整功能，包括 InfoBarArea UI 交互、effect 收集、底部面板占位，以及独立的故事详情面板模块。

## 变更范围

- 新增 `:biz:story:story-panel` 模块（domain + presentation）
- 修改 `:biz:story:infobar:presentation`
- 修改 `settings.gradle.kts`

---

## 新增模块 `:biz:story:story-panel`

### 模块结构

```
biz/story/story-panel/
├── domain/
│   ├── build.gradle.kts
│   └── src/main/kotlin/.../story/storypanel/domain/
│       ├── StoryPanelState.kt
│       ├── StoryPanelEvent.kt
│       ├── StoryPanelEffect.kt
│       └── StoryPanelUseCase.kt
└── presentation/
    ├── build.gradle.kts
    └── src/main/kotlin/.../story/storypanel/presentation/
        ├── StoryPanelScreen.kt
        ├── StoryPanelViewModel.kt
        └── di/
            └── StoryPanelPresentationModule.kt
```

### Domain 层（空实现占位）

| 文件 | 内容 |
|------|------|
| `StoryPanelState.kt` | `data class StoryPanelState(val cardId: String = "") : UiState` |
| `StoryPanelEvent.kt` | `sealed class StoryPanelEvent : UiEvent { data object OnDismiss : StoryPanelEvent() }` |
| `StoryPanelEffect.kt` | `sealed class StoryPanelEffect : UiEffect { data object NavigateBack : StoryPanelEffect() }` |
| `StoryPanelUseCase.kt` | 继承 `BaseUseCase`，`onEvent` 中 `OnDismiss` → `dispatchEffect(NavigateBack)` |

### Presentation 层（MviScreen）

| 文件 | 内容 |
|------|------|
| `StoryPanelViewModel.kt` | 继承 `BaseViewModel<StoryPanelState, StoryPanelEvent, StoryPanelEffect>`，构造 `StoryPanelUseCase` |
| `StoryPanelScreen.kt` | 使用 `MviScreen<StoryPanelViewModel>` 包裹，content 为空占位 `Box`；收集 `NavigateBack` effect 关闭面板 |
| `StoryPanelPresentationModule.kt` | Koin scoped module，提供 `StoryPanelViewModel` |

---

## 修改 `:biz:story:infobar:presentation`

### 模块依赖变更

`build.gradle.kts` 新增：
```kotlin
implementation(project(":biz:story:story-panel:presentation"))
```

### InfoBarArea 变更

#### 1. 左侧故事标题

- `Text(state.storyTitle)` + `clickable` → `sendEvent(OnStoryTitleClicked)`
- 标题为空时不渲染或显示占位符

#### 2. 右侧按钮（从左到右）

- **点赞**：`IconButton`（实心/空心 Heart）+ `Text(likes)` → `sendEvent(OnLikeClicked)`
- **分享**：`IconButton`（Share Icon）→ `sendEvent(OnShareClicked)`
- **评论**：`IconButton`（Comment Icon）→ `sendEvent(OnCommentClicked)`
- **历史**：`IconButton`（History Icon）→ `sendEvent(OnHistoryClicked)`

#### 3. Effect 收集（LaunchedEffect）

```kotlin
LaunchedEffect(viewModel) {
    viewModel.effect.collect { effect ->
        when (effect) {
            is InfoBarEffect.NavigateToStoryDetail -> showDetailPanel = true
            is InfoBarEffect.ShowShareSheet        -> showShareSheet = true
            is InfoBarEffect.NavigateToComments    -> showCommentSheet = true
            is InfoBarEffect.ShowHistory           -> showHistorySheet = true
        }
    }
}
```

#### 4. 底部面板占位

**分享 / 评论 / 历史**：使用 `ModalBottomSheet` 包裹空 `Box`

```kotlin
if (showShareSheet) {
    ModalBottomSheet(onDismissRequest = { showShareSheet = false }) {
        Box(modifier = Modifier.fillMaxSize()) { }
    }
}
```

**详情面板**：使用 `ModalBottomSheet` 包裹 `StoryPanelScreen`

```kotlin
if (showDetailPanel) {
    ModalBottomSheet(onDismissRequest = { showDetailPanel = false }) {
        StoryPanelScreen(cardId = state.cardId)
    }
}
```

### BaseEffect

- `ShowToast` 由 `MviScreen` 统一处理，InfoBarArea 不处理

---

## 模块依赖关系

```
:app
└── :biz:story:presentation
    ├── :biz:story:infobar:presentation
    │   ├── :biz:story:infobar:domain
    │   └── :biz:story:story-panel:presentation  (新增)
    │       └── :biz:story:story-panel:domain    (新增)
    └── ...
```

---

## 文件变更清单

| 文件 | 变更 |
|------|------|
| `settings.gradle.kts` | 新增 `:biz:story:story-panel:domain` 和 `:biz:story:story-panel:presentation` |
| `biz/story/story-panel/domain/build.gradle.kts` | 新增 |
| `biz/story/story-panel/presentation/build.gradle.kts` | 新增 |
| `StoryPanelState.kt` | 新增 |
| `StoryPanelEvent.kt` | 新增 |
| `StoryPanelEffect.kt` | 新增 |
| `StoryPanelUseCase.kt` | 新增 |
| `StoryPanelViewModel.kt` | 新增 |
| `StoryPanelScreen.kt` | 新增 |
| `StoryPanelPresentationModule.kt` | 新增 |
| `biz/story/infobar/presentation/build.gradle.kts` | 新增 story-panel 依赖 |
| `InfoBarArea.kt` | 新增 storyTitle 渲染、按钮点击、effect 收集、ModalBottomSheet 占位 |

---

## 测试策略

- `InfoBarArea` androidTest：验证 storyTitle 点击发送事件、按钮点击发送事件、effect 触发面板显示
- StoryPanel 当前为空实现，domain 层测试可后续补充
