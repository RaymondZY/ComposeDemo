# Input 键盘联动布局设计（UC-10/11 + 全局约束）

**日期**：2026-05-05  
**范围**：`biz/story/input`、`biz/story/presentation`、`app`  
**关联 UC**：全局设计约束、UC-02（clearFocus 补全）、UC-10、UC-11

---

## 背景

UC-01~09 已实现。本文档覆盖调整后 usecases.md 新增的三项工作：

1. 全局约束：禁用系统 `adjustResize`/`adjustPan`，改由应用层通过 IME Insets 精确控制布局
2. 全局约束补全：输入框失焦时必须 `clearFocus()`，清除光标
3. UC-10/11：键盘升降时 StoryCardPage 所有内容整体跟随位移，输入框底部与键盘顶部始终保持 ≥10dp 间距

---

## 架构决策

### 禁用系统自动布局调整

在 `AndroidManifest.xml` 的 `<activity>` 上设置：

```xml
android:windowSoftInputMode="adjustNothing"
```

`MainActivity.enableEdgeToEdge()` 已调用 `WindowCompat.setDecorFitsSystemWindows(window, false)`，配合 `adjustNothing` 后系统完全不干预布局，IME Insets 全权交给 Compose 层处理。

---

### clearFocus 补全（UC-02 全局约束）

**文件**：`InputArea.kt`

**现状**：`LaunchedEffect(state.isFocused)` 在 `isFocused = false` 时只调用 `keyboardController?.hide()`，`BasicTextField` 仍持有焦点，编辑光标可见。

**修复**：

```kotlin
LaunchedEffect(state.isFocused) {
    if (state.isFocused) {
        focusRequester.requestFocus()
        keyboardController?.show()
    } else {
        keyboardController?.hide()
        focusRequester.clearFocus()
    }
}
```

**无循环风险**：`clearFocus()` → `onFocusChanged(false)` → `updateState(isFocused = false)`，StateFlow 值未变不触发重组，LaunchedEffect 不重新执行。

---

### IME 联动偏移（UC-10/11）

**文件**：`StoryCardPage.kt`

#### 数据流

```
WindowInsets.ime.getBottom(density)   → imeBottom（系统 IME 动画帧驱动，每帧更新）
onGloballyPositioned(最外层 Box)      → inputAreaBottomInWindow（px）
                    ↓
windowHeight = LocalView.current.height.toFloat()（px，与 boundsInWindow 同坐标系）
                    ↓
distanceToBottom = windowHeight - inputAreaBottomInWindow
intrusion = max(0f, imeBottom - distanceToBottom - 10.dp.toPx())
                    ↓
graphicsLayer { translationY = -intrusion }  施加在 StoryCardPage 最外层 Box
```

#### 实现要点

- **测量点**：在 `StoryCardPage` 最外层 `Box` 上挂 `onGloballyPositioned { coords -> inputAreaBottom = coords.boundsInWindow().bottom }`，使用 `remember { mutableFloatStateOf(0f) }` 存储
- **IME 高度**：`WindowInsets.ime.getBottom(LocalDensity.current)` 在 Composable 中直接读取，Compose 自动订阅系统 IME 动画帧（API 30+ 帧精确）
- **偏移应用**：`Modifier.graphicsLayer { translationY = -intrusion }`，不影响布局，不触发重组，仅影响绘制层
- **UC-11 还原**：键盘收起时 `imeBottom → 0`，`intrusion → 0`，`translationY → 0`，与 UC-10 共用同一段逻辑，无需额外代码
- **无额外动画**：`WindowInsets.ime` 由系统 IME 动画逐帧驱动，不再叠加 `animateFloatAsState`，避免双重缓动

#### 10dp 安全区语义

```
gap = imeBottom - distanceToBottom
if gap <= 10dp  → 内容不动（intrusion = 0）
if gap >  10dp  → intrusion = gap - 10dp，内容上移 intrusion
```

---

## 修改文件清单

| 文件 | 变更内容 |
|---|---|
| `app/src/main/AndroidManifest.xml` | MainActivity 加 `adjustNothing` |
| `biz/story/input/presentation/.../InputArea.kt` | `LaunchedEffect` 补 `focusRequester.clearFocus()` |
| `biz/story/presentation/.../StoryCardPage.kt` | 加 `onGloballyPositioned` + IME offset 逻辑 |

Domain 层（State/Event/Effect/UseCase）**零改动**。

---

## 验收标准

| 场景 | 预期 |
|---|---|
| 点击输入框，键盘弹出 | 整个 StoryCard（含背景图）随键盘上移；输入框底部距键盘顶部 ≥10dp |
| 键盘高度未侵入 10dp 安全区 | 内容保持不动 |
| 点击输入框外，键盘收起 | 内容平滑还原至原始位置；输入光标消失 |
| 快速连续弹出/收起键盘 | 无抖动、无过冲（overshoot） |
| 横竖屏切换或字体缩放 | `onGloballyPositioned` 重新测量，偏移量自动正确 |
