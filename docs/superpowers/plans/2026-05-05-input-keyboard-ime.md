# Input 键盘联动布局 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 禁用系统 adjustResize/adjustPan，补全 clearFocus，实现键盘升降时 StoryCardPage 整体跟随位移并保持 ≥10dp 安全间距（UC-10/11）。

**Architecture:** AndroidManifest 设置 `adjustNothing` 交出布局控制权；InputArea 补 `focusRequester.clearFocus()` 清除光标；StoryCardPage 通过 `onGloballyPositioned` 测量 InputArea 底部坐标，结合 `WindowInsets.ime` 计算侵入量，经 `graphicsLayer` 驱动整体上移/还原，无需额外动画层。

**Tech Stack:** Jetpack Compose `WindowInsets.ime`、`Modifier.graphicsLayer`、`Modifier.onGloballyPositioned`、`LocalView`、`LocalDensity`

---

## 文件清单

| 操作 | 文件 |
|---|---|
| Modify | `app/src/main/AndroidManifest.xml` |
| Modify | `biz/story/input/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/input/presentation/InputArea.kt` |
| Modify | `biz/story/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/presentation/StoryCardPage.kt` |

Domain 层零改动。

---

## Task 1: 禁用系统软键盘布局自动调整

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 在 MainActivity `<activity>` 标签加 `windowSoftInputMode`**

在 `AndroidManifest.xml` 第 17~25 行的 `<activity>` 标签中，加入 `android:windowSoftInputMode="adjustNothing"`：

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:theme="@style/Theme.TodoListDemo"
    android:windowSoftInputMode="adjustNothing">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

- [ ] **Step 2: 确认构建通过**

```bash
./gradlew :app:assembleDebug
```

预期：`BUILD SUCCESSFUL`，无编译错误。

- [ ] **Step 3: Commit**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat(app): disable adjustResize/adjustPan via adjustNothing"
```

---

## Task 2: 补全失焦时 clearFocus（全局约束 + UC-02）

**Files:**
- Modify: `biz/story/input/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/input/presentation/InputArea.kt`

- [ ] **Step 1: 在 `LaunchedEffect(state.isFocused)` 的 else 分支加 `clearFocus()`**

定位 `InputArea.kt` 第 71~78 行，将 `LaunchedEffect` 改为：

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

原始注释 `// 注意：isFocused=false 时只隐藏键盘，不 clearFocus()，避免与 onFocusChanged 互相触发循环` 可一并删除，已无效。

- [ ] **Step 2: 确认构建通过**

```bash
./gradlew :biz:story:input:presentation:assembleDebug
```

预期：`BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add biz/story/input/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/input/presentation/InputArea.kt
git commit -m "fix(input:presentation): clear focus on keyboard dismiss to remove cursor"
```

---

## Task 3: IME 联动偏移（UC-10/11）

**Files:**
- Modify: `biz/story/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/presentation/StoryCardPage.kt`

- [ ] **Step 1: 在 `StoryCardPage.kt` 顶部补充所需 import**

在现有 import 列表末尾追加：

```kotlin
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
```

- [ ] **Step 2: 在 `StoryCardPage` Composable 函数体顶部声明测量变量和 IME 计算**

在 `val messageViewModel` 等 ViewModel 声明之前插入：

```kotlin
val density = LocalDensity.current
val windowHeight = LocalView.current.height.toFloat()
var inputAreaBottom by remember { mutableFloatStateOf(windowHeight) }
val imeBottom = WindowInsets.ime.getBottom(density).toFloat()
val safetyMarginPx = with(density) { 10.dp.toPx() }
val intrusion = maxOf(0f, imeBottom - (windowHeight - inputAreaBottom) - safetyMarginPx)
```

- [ ] **Step 3: 给最外层 `Box` 加 `onGloballyPositioned` 和 `graphicsLayer`**

找到 `StoryCardPage` 中最外层的 `Box(modifier = Modifier.fillMaxSize())` 这一行（当前第 51 行），改为：

```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned { coords ->
            inputAreaBottom = coords.boundsInWindow().bottom
        }
        .graphicsLayer { translationY = -intrusion }
) {
```

> **说明**：`onGloballyPositioned` 测量整个 StoryCardPage 容器的底部在窗口中的 Y 坐标（px）。`imeBottom` 是键盘高度（从窗口底部往上量）。`windowHeight - inputAreaBottom` 是容器底部距窗口底部的距离（通常为导航栏高度 + 额外间距）。`intrusion` 是键盘超出 10dp 安全区的部分，即内容需要上移的量。

- [ ] **Step 4: 确认构建通过**

```bash
./gradlew :biz:story:presentation:assembleDebug
```

预期：`BUILD SUCCESSFUL`，无未解析引用。

- [ ] **Step 5: 手动 E2E 验证**

安装到设备或模拟器运行，逐项检查：

| 场景 | 预期结果 |
|---|---|
| 点击输入框，键盘弹出 | 整个 StoryCard（含背景图）跟随键盘上移；输入框底部与键盘顶部保持 ≥10dp |
| 键盘高度未侵入 10dp 安全区时 | 内容不动 |
| 点击输入框外，键盘收起 | 内容平滑还原至原始位置；输入光标消失（Task 2 效果） |
| 快速连续弹出/收起 | 无抖动、无过冲（overshoot） |

- [ ] **Step 6: Commit**

```bash
git add biz/story/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/presentation/StoryCardPage.kt
git commit -m "feat(story:presentation): IME-driven layout offset for UC-10/11"
```
