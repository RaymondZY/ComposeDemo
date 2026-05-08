# Story InfoBar Presentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 InfoBar presentation 层完整功能，包括 effect 收集、底部面板占位、故事标题点击，以及新增 story-panel 模块。

**Architecture:** InfoBarArea 内自包含 effect 收集与面板管理；story-panel 为独立 MviScreen 模块，通过 ModalBottomSheet 嵌入。

**Tech Stack:** Kotlin, Jetpack Compose, Material3 ModalBottomSheet, Koin, JUnit

---

## File Map

| 文件 | 责任 |
|------|------|
| `settings.gradle.kts` | 注册 `:biz:story:story-panel:domain` 和 `:biz:story:story-panel:presentation` |
| `biz/story/story-panel/domain/build.gradle.kts` | 新模块构建配置 |
| `StoryPanelState.kt` | 空 State 占位 |
| `StoryPanelEvent.kt` | 空 Event 占位 |
| `StoryPanelEffect.kt` | 空 Effect 占位 |
| `StoryPanelUseCase.kt` | 空 UseCase 占位 |
| `biz/story/story-panel/presentation/build.gradle.kts` | 新模块构建配置 |
| `StoryPanelViewModel.kt` | ViewModel 桥梁 |
| `StoryPanelScreen.kt` | MviScreen 空页面 |
| `StoryPanelPresentationModule.kt` | Koin DI 模块 |
| `biz/story/infobar/presentation/build.gradle.kts` | 新增 story-panel 依赖 |
| `ComposeDemoApp.kt` | 注册 storyPanelPresentationModule |
| `InfoBarArea.kt` | 新增 storyTitle、effect 收集、ModalBottomSheet |
| `StoryCardPage.kt` | 修改 InfoBarArea 调用，传入 cardId |

---

### Task 1: 创建 story-panel domain 模块

**Files:**
- Create: `biz/story/story-panel/domain/build.gradle.kts`
- Create: `biz/story/story-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/storypanel/domain/StoryPanelState.kt`
- Create: `biz/story/story-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/storypanel/domain/StoryPanelEvent.kt`
- Create: `biz/story/story-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/storypanel/domain/StoryPanelEffect.kt`
- Create: `biz/story/story-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/storypanel/domain/StoryPanelUseCase.kt`

- [ ] **Step 1: 创建 build.gradle.kts**

路径：`biz/story/story-panel/domain/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":scaffold:core"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 2: 创建 StoryPanelState.kt**

路径：`biz/story/story-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/storypanel/domain/StoryPanelState.kt`

```kotlin
package zhaoyun.example.composedemo.story.storypanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class StoryPanelState(
    val cardId: String = "",
) : UiState
```

- [ ] **Step 3: 创建 StoryPanelEvent.kt**

路径：`biz/story/story-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/storypanel/domain/StoryPanelEvent.kt`

```kotlin
package zhaoyun.example.composedemo.story.storypanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class StoryPanelEvent : UiEvent {
    data object OnDismiss : StoryPanelEvent()
}
```

- [ ] **Step 4: 创建 StoryPanelEffect.kt**

路径：`biz/story/story-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/storypanel/domain/StoryPanelEffect.kt`

```kotlin
package zhaoyun.example.composedemo.story.storypanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect

sealed class StoryPanelEffect : UiEffect {
    data object NavigateBack : StoryPanelEffect()
}
```

- [ ] **Step 5: 创建 StoryPanelUseCase.kt**

路径：`biz/story/story-panel/domain/src/main/kotlin/zhaoyun/example/composedemo/story/storypanel/domain/StoryPanelUseCase.kt`

```kotlin
package zhaoyun.example.composedemo.story.storypanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class StoryPanelUseCase(
    stateHolder: StateHolder<StoryPanelState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<StoryPanelState, StoryPanelEvent, StoryPanelEffect>(
    stateHolder,
    serviceRegistry,
) {
    override suspend fun onEvent(event: StoryPanelEvent) {
        when (event) {
            is StoryPanelEvent.OnDismiss -> {
                dispatchEffect(StoryPanelEffect.NavigateBack)
            }
        }
    }
}
```

- [ ] **Step 6: 编译检查**

Run: `./gradlew :biz:story:story-panel:domain:compileKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 2: 创建 story-panel presentation 模块

**Files:**
- Create: `biz/story/story-panel/presentation/build.gradle.kts`
- Create: `biz/story/story-panel/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/storypanel/presentation/StoryPanelViewModel.kt`
- Create: `biz/story/story-panel/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/storypanel/presentation/StoryPanelScreen.kt`
- Create: `biz/story/story-panel/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/storypanel/presentation/di/StoryPanelPresentationModule.kt`

- [ ] **Step 1: 创建 build.gradle.kts**

路径：`biz/story/story-panel/presentation/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "zhaoyun.example.composedemo.story.storypanel.presentation"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    api(project(":biz:story:story-panel:domain"))
    implementation(project(":scaffold:android"))

    implementation(libs.koin.androidx.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 2: 创建 StoryPanelViewModel.kt**

路径：`biz/story/story-panel/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/storypanel/presentation/StoryPanelViewModel.kt`

```kotlin
package zhaoyun.example.composedemo.story.storypanel.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.storypanel.domain.StoryPanelEffect
import zhaoyun.example.composedemo.story.storypanel.domain.StoryPanelEvent
import zhaoyun.example.composedemo.story.storypanel.domain.StoryPanelState
import zhaoyun.example.composedemo.story.storypanel.domain.StoryPanelUseCase

class StoryPanelViewModel(
    stateHolder: StateHolder<StoryPanelState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<StoryPanelState, StoryPanelEvent, StoryPanelEffect>(
    stateHolder,
    serviceRegistry,
    { holder, registry ->
        StoryPanelUseCase(
            stateHolder = holder,
            serviceRegistry = registry,
        )
    },
)
```

- [ ] **Step 3: 创建 StoryPanelPresentationModule.kt**

路径：`biz/story/story-panel/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/storypanel/presentation/di/StoryPanelPresentationModule.kt`

```kotlin
package zhaoyun.example.composedemo.story.storypanel.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.android.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.storypanel.domain.StoryPanelState
import zhaoyun.example.composedemo.story.storypanel.presentation.StoryPanelViewModel

val storyPanelPresentationModule = module {
    scope(MviKoinScopes.Item) {
        viewModel { (stateHolder: StateHolder<StoryPanelState>) ->
            StoryPanelViewModel(
                stateHolder = stateHolder,
                serviceRegistry = get(),
            )
        }
    }
}
```

- [ ] **Step 4: 创建 StoryPanelScreen.kt**

路径：`biz/story/story-panel/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/storypanel/presentation/StoryPanelScreen.kt`

```kotlin
package zhaoyun.example.composedemo.story.storypanel.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import org.koin.core.parameter.parametersOf
import zhaoyun.example.composedemo.scaffold.android.MviScreen
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.story.storypanel.domain.StoryPanelEffect
import zhaoyun.example.composedemo.story.storypanel.domain.StoryPanelState

@Composable
fun StoryPanelScreen(
    cardId: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MviScreen<StoryPanelViewModel>(
        parameters = {
            parametersOf(StoryPanelState(cardId = cardId).toStateHolder())
        },
    ) { viewModel ->
        LaunchedEffect(viewModel) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is StoryPanelEffect.NavigateBack -> onNavigateBack()
                }
            }
        }

        Box(
            modifier = modifier.fillMaxSize(),
        ) {
            // 空页面占位，保留后续扩展逻辑
        }
    }
}
```

- [ ] **Step 5: 编译检查**

Run: `./gradlew :biz:story:story-panel:presentation:compileKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: 注册模块并配置依赖

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `biz/story/infobar/presentation/build.gradle.kts`
- Modify: `app/src/main/java/zhaoyun/example/composedemo/ComposeDemoApp.kt`

- [ ] **Step 1: 修改 settings.gradle.kts**

在文件末尾（`include(":service:feed:mock")` 之后）新增两行：

```kotlin
include(":biz:story:story-panel:domain")
include(":biz:story:story-panel:presentation")
```

- [ ] **Step 2: 修改 infobar presentation build.gradle.kts**

在 `dependencies` 块中，在 `api(project(":biz:story:infobar:domain"))` 下方新增：

```kotlin
implementation(project(":biz:story:story-panel:presentation"))
```

- [ ] **Step 3: 修改 ComposeDemoApp.kt**

新增 import：

```kotlin
import zhaoyun.example.composedemo.story.storypanel.presentation.di.storyPanelPresentationModule
```

在 `modules(...)` 中的 list 里，在 `backgroundPresentationModule` 之前新增：

```kotlin
storyPanelPresentationModule,
```

---

### Task 4: 修改 InfoBarArea

**Files:**
- Modify: `biz/story/infobar/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/infobar/presentation/InfoBarArea.kt`

- [ ] **Step 1: 修改 InfoBarArea.kt**

替换整个文件内容。关键变更：
- 新增 `cardId` 参数
- 左侧改为 `storyTitle`（可点击发送 `OnStoryTitleClicked`）
- 删除作者信息相关 UI
- 新增 `LaunchedEffect` 收集 effect
- 新增 `ModalBottomSheet` 处理分享/评论/历史/详情

```kotlin
package zhaoyun.example.composedemo.story.infobar.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarEffect
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarEvent
import zhaoyun.example.composedemo.story.storypanel.presentation.StoryPanelScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoBarArea(
    viewModel: InfoBarViewModel,
    cardId: String,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showShareSheet by remember { mutableStateOf(false) }
    var showCommentSheet by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showDetailPanel by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is InfoBarEffect.ShowShareSheet -> showShareSheet = true
                is InfoBarEffect.NavigateToComments -> showCommentSheet = true
                is InfoBarEffect.ShowHistory -> showHistorySheet = true
                is InfoBarEffect.NavigateToStoryDetail -> showDetailPanel = true
            }
        }
    }

    Column(
        modifier = modifier.padding(top = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左侧：故事标题（可点击）
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.sendEvent(InfoBarEvent.OnStoryTitleClicked) },
            ) {
                Text(
                    text = state.storyTitle.ifEmpty { "未命名故事" },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = Color.White,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 右侧：横向排列的图标按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                IconButtonHorizontal(
                    icon = {
                        Icon(
                            imageVector = if (state.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (state.isLiked) Color(0xFFFF6B6B) else Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    count = formatCount(state.likes),
                    onClick = { viewModel.sendEvent(InfoBarEvent.OnLikeClicked) },
                )

                IconButtonHorizontal(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Send,
                            contentDescription = "Share",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    count = formatCount(state.shares),
                    onClick = { viewModel.sendEvent(InfoBarEvent.OnShareClicked) },
                )

                IconButtonHorizontal(
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.ThumbUp,
                            contentDescription = "Comment",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    },
                    count = formatCount(state.comments),
                    onClick = { viewModel.sendEvent(InfoBarEvent.OnCommentClicked) },
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { viewModel.sendEvent(InfoBarEvent.OnHistoryClicked) },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = "History",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "历史",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }

    if (showShareSheet) {
        ModalBottomSheet(
            onDismissRequest = { showShareSheet = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) { }
        }
    }

    if (showCommentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCommentSheet = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) { }
        }
    }

    if (showHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Box(modifier = Modifier.fillMaxSize()) { }
        }
    }

    if (showDetailPanel) {
        ModalBottomSheet(
            onDismissRequest = { showDetailPanel = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            StoryPanelScreen(
                cardId = cardId,
                onNavigateBack = { showDetailPanel = false },
            )
        }
    }
}

@Composable
private fun IconButtonHorizontal(
    icon: @Composable () -> Unit,
    count: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        icon()
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = count,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = Color.White,
        )
    }
}

private fun formatCount(count: Int): String {
    return when {
        count >= 10000 -> "${count / 10000}万"
        count >= 1000 -> "${count / 1000}k"
        else -> count.toString()
    }
}
```

---

### Task 5: 修改 StoryCardPage 中 InfoBarArea 的调用

**Files:**
- Modify: `biz/story/presentation/src/main/kotlin/zhaoyun/example/composedemo/story/presentation/StoryCardPage.kt`

- [ ] **Step 1: 修改 StoryCardPage.kt 中 InfoBarArea 调用**

找到 `InfoBarArea(viewModel = infoBarViewModel)` 所在行，修改为：

```kotlin
InfoBarArea(
    viewModel = infoBarViewModel,
    cardId = card.cardId,
)
```

---

### Task 6: 编译检查

- [ ] **Step 1: 全项目编译**

Run: `export JAVA_HOME=/Users/bytedance/Library/Java/JavaVirtualMachines/jbr-21.0.8/Contents/Home && ./gradlew :biz:story:infobar:presentation:compileKotlin :biz:story:presentation:compileKotlin :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

## Self-Review Checklist

- [ ] Spec coverage: 全部 design doc 需求有对应 task
- [ ] Placeholder scan: 无 TBD、TODO
- [ ] Type consistency: `InfoBarArea` 新增 `cardId` 参数，与 `StoryCardPage` 调用一致
- [ ] `StoryPanelScreen` 使用 `MviScreen` 包裹，符合用户要求
