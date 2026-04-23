# Feed 首页设计文档

## 1. 项目背景与目标

在 `:biz:feed` 模块中构建一个 4Tab + 中间按钮的首页结构，作为应用的 Launcher Activity。首页第一个 Tab（首页）承载上下滑动的 Feed 流，Feed 流中的卡片为 Story 类型。其他 3 个 Tab（发现/消息/我的）当前为空白占位页。

核心目标：
- 按项目约定的 MVI 架构开发
- 遵循开发顺序：状态建模 → UseCase 设计 → UI 展示
- 每步都有明确的测试用例
- Story 内部采用原子 MVI 结构，子模块独立且通过 DelegateReducer 共享状态

## 2. 架构原则

1. **分层独立 MVI**：Home / Feed / Story 各自有独立的 domain + presentation 模块
2. **Feed 通用框架化**：FeedUseCase 只管理 refresh / loadMore / preload，不感知 Card 具体业务
3. **Story 原子 MVI**：Story 内部每个子模块（Message / InfoBar / Input / Background）都有独立的 State / Event / Effect / UseCase / ViewModel
4. **DelegateReducer 状态共享**：StoryCardViewModel 作为主状态管理中心，为每个子模块创建 DelegateReducer；子 ViewModel 只接收 `Reducer<子State>`，完全解耦于 StoryCardState
5. **数据驱动 UI**：UI 统一订阅 `storyViewModel.state`，子 ViewModel 仅用于事件处理和 Effect 发射

## 3. 模块结构

```
:biz:home
  ├── :domain
  │     ├── HomeState.kt
  │     ├── HomeEvent.kt
  │     ├── HomeEffect.kt
  │     └── HomeUseCase.kt
  └── :presentation
        ├── HomeViewModel.kt
        ├── HomeScreen.kt
        ├── HomePage.kt
        └── di/
              └── HomePresentationModule.kt

:biz:feed
  ├── :domain
  │     ├── FeedState.kt
  │     ├── FeedEvent.kt
  │     ├── FeedEffect.kt
  │     └── FeedUseCase.kt
  └── :presentation
        ├── FeedViewModel.kt
        ├── FeedScreen.kt
        ├── FeedPage.kt
        └── di/
              └── FeedPresentationModule.kt

:biz:story
  ├── :domain
  │     ├── StoryCardState.kt
  │     ├── StoryCardEvent.kt
  │     ├── StoryCardEffect.kt
  │     └── StoryCardUseCase.kt
  └── :presentation
        ├── StoryCardViewModel.kt
        ├── StoryCardPage.kt
        └── di/
              └── StoryPresentationModule.kt

:biz:story:message
  ├── :domain
  │     ├── MessageState.kt
  │     ├── MessageEvent.kt
  │     ├── MessageEffect.kt
  │     └── MessageUseCase.kt
  └── :presentation
        ├── MessageViewModel.kt
        ├── MessageArea.kt
        └── di/
              └── MessagePresentationModule.kt

:biz:story:infobar
  ├── :domain
  │     ├── InfoBarState.kt
  │     ├── InfoBarEvent.kt
  │     ├── InfoBarEffect.kt
  │     └── InfoBarUseCase.kt
  └── :presentation
        ├── InfoBarViewModel.kt
        ├── InfoBarArea.kt
        └── di/
              └── InfoBarPresentationModule.kt

:biz:story:input
  ├── :domain
  │     ├── InputState.kt
  │     ├── InputEvent.kt
  │     ├── InputEffect.kt
  │     └── InputUseCase.kt
  └── :presentation
        ├── InputViewModel.kt
        ├── InputArea.kt
        └── di/
              └── InputPresentationModule.kt

:biz:story:background
  ├── :domain
  │     ├── BackgroundState.kt
  │     ├── BackgroundEvent.kt
  │     ├── BackgroundEffect.kt
  │     └── BackgroundUseCase.kt
  └── :presentation
        ├── BackgroundViewModel.kt
        ├── StoryBackground.kt
        └── di/
              └── BackgroundPresentationModule.kt

:service:feed
  ├── :api
  │     ├── FeedRepository.kt
  │     └── model/
  │           ├── FeedCard.kt
  │           └── StoryCard.kt
  └── :mock
        └── FakeFeedRepository.kt
```

### 3.1 依赖关系

- `:biz:home:presentation` → `:biz:home:domain` + `:biz:feed:presentation` + `:scaffold:android`
- `:biz:feed:presentation` → `:biz:feed:domain` + `:biz:story:presentation` + `:scaffold:android`
- `:biz:story:presentation` → `:biz:story:domain` + `:biz:story:message:presentation` + `:biz:story:infobar:presentation` + `:biz:story:input:presentation` + `:biz:story:background:presentation` + `:scaffold:android`
- `:biz:story:*:presentation` → 对应的 `:domain` + `:scaffold:android`
- `:biz:feed:domain` → `:service:feed:api` + `:scaffold:core`
- `:biz:story:domain` → `:service:feed:api` + `:scaffold:core`
- `:biz:story:*:domain` → `:scaffold:core`

## 4. Home MVI 设计

### 4.1 State / Event / Effect

```kotlin
data class HomeState(
    val selectedTab: Tab = Tab.HOME,
    val tabBadges: Map<Tab, TabBadge> = emptyMap(),
) : UiState

data class TabBadge(
    val showRedDot: Boolean = false,
    val unreadCount: Int = 0,
) {
    val hasBadge: Boolean get() = showRedDot || unreadCount > 0
}

enum class Tab {
    HOME, DISCOVER, MESSAGE, PROFILE
}

sealed class HomeEvent : UiEvent {
    data class OnTabSelected(val tab: Tab) : HomeEvent()
    data object OnCenterButtonClicked : HomeEvent()
    data class OnBadgeUpdated(val tab: Tab, val badge: TabBadge) : HomeEvent()
}

sealed class HomeEffect : UiEffect
```

### 4.2 HomeUseCase

```kotlin
class HomeUseCase : BaseUseCase<HomeState, HomeEvent, HomeEffect>(HomeState()) {
    override suspend fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnTabSelected -> {
                if (event.tab != state.value.selectedTab) {
                    updateState { it.copy(selectedTab = event.tab) }
                }
            }
            is HomeEvent.OnCenterButtonClicked -> {
                // 空实现，后续扩展
            }
            is HomeEvent.OnBadgeUpdated -> {
                updateState {
                    it.copy(tabBadges = it.tabBadges + (event.tab to event.badge))
                }
            }
        }
    }
}
```

### 4.3 HomeUseCase 测试用例

| # | 测试名 | 预期行为 |
|---|--------|----------|
| 1 | `初始状态默认选中HOME` | `state.selectedTab == Tab.HOME`, `tabBadges.isEmpty()` |
| 2 | `切换Tab状态更新` | `OnTabSelected(DISCOVER)` 后 `selectedTab == DISCOVER` |
| 3 | `重复选择同一Tab不触发状态变更` | 连续两次 `OnTabSelected(HOME)`，state 对象引用不变 |
| 4 | `点击中间按钮不改变selectedTab` | `OnCenterButtonClicked` 后 `selectedTab` 保持原值 |
| 5 | `更新角标后tabBadges包含数据` | `OnBadgeUpdated(MESSAGE, TabBadge(unreadCount=3))` 后 `tabBadges[MESSAGE]?.unreadCount == 3` |
| 6 | `更新不同Tab角标互不覆盖` | 先更新 MESSAGE，再更新 DISCOVER，两者都在 Map 中 |
| 7 | `连续切换Tab状态始终反映最新选择` | 快速发送多个 `OnTabSelected`，最终状态匹配最后一条 |

## 5. Feed MVI 设计

### 5.1 数据模型（`:service:feed:api`）

```kotlin
interface FeedCard {
    val cardId: String
    val cardType: String
}

data class StoryCard(
    override val cardId: String,
    override val cardType: String = "story",
    val backgroundImageUrl: String,
    val characterName: String,
    val characterSubtitle: String?,
    val dialogueText: String,
    val storyTitle: String,
    val creatorName: String,
    val creatorHandle: String,
    val likes: Int,
    val shares: Int,
    val comments: Int,
    val isLiked: Boolean = false,
) : FeedCard
```

### 5.2 State / Event / Effect

```kotlin
data class FeedState(
    val cards: List<FeedCard> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val currentPage: Int = 0,
    val hasMore: Boolean = true,
) : UiState

sealed class FeedEvent : UiEvent {
    data object OnRefresh : FeedEvent()
    data object OnLoadMore : FeedEvent()
    data class OnPreload(val index: Int) : FeedEvent()
}

sealed class FeedEffect : UiEffect {
    data class ShowError(val message: String) : FeedEffect()
}
```

### 5.3 FeedUseCase

```kotlin
class FeedUseCase(
    private val feedRepository: FeedRepository
) : BaseUseCase<FeedState, FeedEvent, FeedEffect>(FeedState()) {

    override suspend fun onEvent(event: FeedEvent) {
        when (event) {
            is FeedEvent.OnRefresh -> loadFeed(refresh = true)
            is FeedEvent.OnLoadMore -> loadFeed(refresh = false)
            is FeedEvent.OnPreload -> {
                val current = state.value
                if (event.index >= current.cards.size - 2
                    && current.hasMore
                    && !current.isLoading
                    && !current.isRefreshing
                ) {
                    loadFeed(refresh = false)
                }
            }
        }
    }

    private suspend fun loadFeed(refresh: Boolean) {
        val current = state.value
        if (current.isLoading || current.isRefreshing) return

        if (refresh) {
            updateState { it.copy(isRefreshing = true, errorMessage = null) }
        } else {
            updateState { it.copy(isLoading = true, errorMessage = null) }
        }

        val page = if (refresh) 0 else current.currentPage
        feedRepository.fetchFeed(page = page, pageSize = PAGE_SIZE)
            .onSuccess { cards ->
                val newCards = if (refresh) cards else current.cards + cards
                updateState {
                    it.copy(
                        cards = newCards,
                        isRefreshing = false,
                        isLoading = false,
                        currentPage = if (refresh) 1 else page + 1,
                        hasMore = cards.size >= PAGE_SIZE,
                    )
                }
            }
            .onFailure { error ->
                updateState {
                    it.copy(
                        isRefreshing = false,
                        isLoading = false,
                        errorMessage = error.message,
                    )
                }
            }
    }

    companion object {
        private const val PAGE_SIZE = 10
    }
}
```

### 5.4 FeedRepository 接口

```kotlin
interface FeedRepository {
    suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>>
}
```

### 5.5 FeedUseCase 测试用例

| # | 测试名 | 预期行为 |
|---|--------|----------|
| 8 | `初始状态为空列表且不加载` | `cards.isEmpty()`, `isLoading == false`, `isRefreshing == false` |
| 9 | `刷新事件触发刷新状态` | `OnRefresh` 后 `isRefreshing == true`, `errorMessage == null` |
| 10 | `刷新成功填充数据并更新状态` | Repository 返回 3 条，`cards.size == 3`, `isRefreshing == false`, `currentPage == 1` |
| 11 | `刷新失败保留旧数据并显示错误` | Repository 抛异常，`errorMessage != null`, `isRefreshing == false`, `cards` 保持刷新前值 |
| 12 | `加载更多追加数据并增加page` | `OnLoadMore` 后 `currentPage == 2`, `cards.size` 为旧数量+新数量 |
| 13 | `加载更多无数据时hasMore为false` | Repository 返回空列表，`hasMore == false` |
| 14 | `预加载在接近末尾时触发加载` | `OnPreload(cards.size - 2)` 且 `hasMore == true` 时内部触发 `loadMore` |
| 15 | `预加载在已有加载进行时忽略` | `isLoading == true` 时 `OnPreload` 不触发新加载 |
| 16 | `刷新时忽略重复的刷新请求` | `isRefreshing == true` 时再次 `OnRefresh` 不触发新加载 |

## 6. StoryCard MVI 设计

### 6.1 嵌套 State 结构

```kotlin
data class StoryCardState(
    val background: BackgroundState = BackgroundState(),
    val message: MessageState = MessageState(),
    val infoBar: InfoBarState = InfoBarState(),
    val input: InputState = InputState(),
) : UiState

data class BackgroundState(
    val backgroundImageUrl: String = "",
) : UiState

data class MessageState(
    val characterName: String = "",
    val characterSubtitle: String? = null,
    val dialogueText: String = "",
    val isExpanded: Boolean = false,
) : UiState

data class InfoBarState(
    val storyTitle: String = "",
    val creatorName: String = "",
    val creatorHandle: String = "",
    val likes: Int = 0,
    val shares: Int = 0,
    val comments: Int = 0,
    val isLiked: Boolean = false,
) : UiState

data class InputState(
    val hintText: String = "自由输入...",
    val isFocused: Boolean = false,
) : UiState
```

### 6.2 StoryCardEvent（分层命名空间）

```kotlin
sealed class StoryCardEvent : UiEvent {
    data object OnRefresh : StoryCardEvent()
    data object OnLoadMore : StoryCardEvent()

    sealed class Message : StoryCardEvent() {
        data object OnDialogueClicked : Message()
    }

    sealed class InfoBar : StoryCardEvent() {
        data object OnLikeClicked : InfoBar()
        data object OnShareClicked : InfoBar()
        data object OnCommentClicked : InfoBar()
        data object OnHistoryClicked : InfoBar()
    }

    sealed class Input : StoryCardEvent() {
        data object OnFocused : Input()
        data object OnInputClicked : Input()
        data object OnSendClicked : Input()
    }
}
```

### 6.3 StoryCardEffect（分层命名空间）

```kotlin
sealed class StoryCardEffect : UiEffect {
    sealed class InfoBar : StoryCardEffect() {
        data class ShowShareSheet(val cardId: String) : InfoBar()
        data class NavigateToComments(val cardId: String) : InfoBar()
        data class ShowHistory(val cardId: String) : InfoBar()
    }

    sealed class Input : StoryCardEffect() {
        data class NavigateToChat(val cardId: String) : Input()
        data class SendMessage(val cardId: String, val text: String) : Input()
    }
}
```

### 6.4 StoryCardViewModel（状态管理中心）

```kotlin
class StoryCardViewModel : BaseViewModel<StoryCardState, StoryCardEvent, StoryCardEffect>(
    StoryCardState(),
    StoryCardUseCase()
) {
    val messageReducer: Reducer<MessageState> by lazy { createMessageReducer() }
    val infoBarReducer: Reducer<InfoBarState> by lazy { createInfoBarReducer() }
    val inputReducer: Reducer<InputState> by lazy { createInputReducer() }
    val backgroundReducer: Reducer<BackgroundState> by lazy { createBackgroundReducer() }

    private fun createMessageReducer(): Reducer<MessageState> {
        val messageStateFlow = MutableStateFlow(state.value.message)
        return createDelegateReducer(
            stateFlow = messageStateFlow,
            onReduce = { transform ->
                val newMessage = transform(state.value.message)
                updateState { it.copy(message = newMessage) }
                messageStateFlow.value = newMessage
            }
        )
    }

    // createInfoBarReducer / createInputReducer / createBackgroundReducer 同理
}
```

### 6.5 子 ViewModel（原子 MVI，完全解耦）

```kotlin
// MessageViewModel
class MessageViewModel(
    messageReducer: Reducer<MessageState>,
) : BaseViewModel<MessageState, MessageEvent, MessageEffect>(
    MessageState(),
    MessageUseCase()
) {
    override fun createReducer(initialState: MessageState): Reducer<MessageState> = messageReducer
}

// InfoBarViewModel
class InfoBarViewModel(
    infoBarReducer: Reducer<InfoBarState>,
) : BaseViewModel<InfoBarState, InfoBarEvent, InfoBarEffect>(
    InfoBarState(),
    InfoBarUseCase()
) {
    override fun createReducer(initialState: InfoBarState): Reducer<InfoBarState> = infoBarReducer
}

// InputViewModel
class InputViewModel(
    inputReducer: Reducer<InputState>,
) : BaseViewModel<InputState, InputEvent, InputEffect>(
    InputState(),
    InputUseCase()
) {
    override fun createReducer(initialState: InputState): Reducer<InputState> = inputReducer
}

// BackgroundViewModel
class BackgroundViewModel(
    backgroundReducer: Reducer<BackgroundState>,
) : BaseViewModel<BackgroundState, BackgroundEvent, BackgroundEffect>(
    BackgroundState(),
    BackgroundUseCase()
) {
    override fun createReducer(initialState: BackgroundState): Reducer<BackgroundState> = backgroundReducer
}
```

### 6.6 StoryCard 子 UseCase 测试用例

| # | 测试名 | 预期行为 |
|---|--------|----------|
| 17 | `StoryCardViewModel创建子Reducer成功` | `messageReducer.state.value` 初始值与 `StoryCardState().message` 一致 |
| 18 | `MessageViewModel通过DelegateReducer写回主状态` | `messageViewModel.onEvent(MessageEvent.OnDialogueClicked)` 后，`storyViewModel.state.value.message.isExpanded == true` |
| 19 | `InfoBarViewModel点赞切换isLiked并修改计数` | `infoBarViewModel.onEvent(InfoBarEvent.OnLikeClicked)` 后，`storyViewModel.state.value.infoBar.isLiked == true`, `likes` 计数正确 |
| 20 | `InfoBarViewModel发射Effect` | `infoBarViewModel.onEvent(InfoBarEvent.OnShareClicked)` 后 effects 包含 `InfoBar.ShowShareSheet(cardId)` |
| 21 | `各子ViewModel忽略非自己命名空间的事件` | `messageViewModel` 收到 `InfoBarEvent`（通过 StoryCardEvent 透传时），`state.message` 不变 |
| 22 | `多子ViewModel共享主Reducer互不干扰` | `messageViewModel` 更新 `message.isExpanded` 不影响 `infoBarViewModel.state.value.infoBar` |

## 7. UI 结构与 Composable 设计

### 7.1 HomeScreen

```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = koinViewModel()) {
    MviScreen(viewModel = viewModel) { state, onEvent ->
        HomePage(state = state, onEvent = onEvent)
    }
}

@Composable
fun HomePage(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
) {
    Scaffold(
        topBar = { /* 顶部工具栏占位 */ },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = state.selectedTab,
                tabBadges = state.tabBadges,
                onTabSelected = { onEvent(HomeEvent.OnTabSelected(it)) },
                onCenterButtonClicked = { onEvent(HomeEvent.OnCenterButtonClicked) },
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (state.selectedTab) {
                Tab.HOME -> FeedScreen()
                Tab.DISCOVER -> DiscoverPage()
                Tab.MESSAGE -> MessagePage()
                Tab.PROFILE -> ProfilePage()
            }
        }
    }
}
```

### 7.2 FeedScreen

```kotlin
@Composable
fun FeedScreen(viewModel: FeedViewModel = koinViewModel()) {
    MviScreen(
        viewModel = viewModel,
        initEvent = FeedEvent.OnRefresh,
    ) { state, onEvent ->
        FeedPage(state = state, onEvent = onEvent)
    }
}

@Composable
fun FeedPage(
    state: FeedState,
    onEvent: (FeedEvent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(
            state = rememberPagerState(pageCount = { state.cards.size }),
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            state.cards.getOrNull(page)?.let { card ->
                when (card) {
                    is StoryCard -> StoryCardPage(card = card)
                    else -> PlaceholderCard()
                }
            }
            LaunchedEffect(page) { onEvent(FeedEvent.OnPreload(page)) }
        }

        if (state.isRefreshing) { LinearProgressIndicator(...) }
        if (state.isLoading) { CircularProgressIndicator(...) }
    }
}
```

### 7.3 StoryCardPage（组装层）

```kotlin
@Composable
fun StoryCardPage(
    card: StoryCard,
    onEffect: (StoryCardEffect) -> Unit = {},
) {
    val storyViewModel: StoryCardViewModel = koinViewModel { parametersOf(card) }

    val messageViewModel: MessageViewModel = koinViewModel {
        parametersOf(storyViewModel.messageReducer)
    }
    val infoBarViewModel: InfoBarViewModel = koinViewModel {
        parametersOf(storyViewModel.infoBarReducer)
    }
    val inputViewModel: InputViewModel = koinViewModel {
        parametersOf(storyViewModel.inputReducer)
    }
    val backgroundViewModel: BackgroundViewModel = koinViewModel {
        parametersOf(storyViewModel.backgroundReducer)
    }

    val state by storyViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        merge(
            messageViewModel.effect,
            infoBarViewModel.effect,
            inputViewModel.effect,
            backgroundViewModel.effect,
        ).collect { onEffect(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        StoryBackground(state = state.background)
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.weight(1f))
            MessageArea(
                state = state.message,
                onEvent = { messageViewModel.onEvent(it) },
            )
            InfoBarArea(
                state = state.infoBar,
                onEvent = { infoBarViewModel.onEvent(it) },
            )
            InputArea(
                state = state.input,
                onEvent = { inputViewModel.onEvent(it) },
            )
        }
    }
}
```

### 7.4 占位页面

```kotlin
@Composable
fun DiscoverPage() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("发现")
    }
}

@Composable
fun MessagePage() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("消息")
    }
}

@Composable
fun ProfilePage() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("我的")
    }
}
```

## 8. Activity 与 Manifest

### 8.1 FeedActivity（新 Launcher）

```kotlin
class FeedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeDemoTheme {
                HomeScreen()
            }
        }
    }
}
```

### 8.2 AndroidManifest 声明

```xml
<activity
    android:name=".feed.FeedActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

`MainActivity` 保持不动。

## 9. DI 模块（Koin）

### 9.1 Domain 模块

```kotlin
// :biz:home:domain
val homeDomainModule = module {
    factory { HomeUseCase() }
}

// :biz:feed:domain
val feedDomainModule = module {
    factory { FeedUseCase(get()) }
}

// :biz:story:domain
val storyDomainModule = module {
    factory { StoryCardUseCase() }
}

// :biz:story:message:domain
val messageDomainModule = module {
    factory { MessageUseCase() }
}

// :biz:story:infobar:domain
val infoBarDomainModule = module {
    factory { InfoBarUseCase() }
}

// :biz:story:input:domain
val inputDomainModule = module {
    factory { InputUseCase() }
}

// :biz:story:background:domain
val backgroundDomainModule = module {
    factory { BackgroundUseCase() }
}
```

### 9.2 Presentation 模块

```kotlin
// :biz:home:presentation
val homePresentationModule = module {
    viewModel { HomeViewModel(get()) }
}

// :biz:feed:presentation
val feedPresentationModule = module {
    viewModel { FeedViewModel(get()) }
}

// :biz:story:presentation
val storyPresentationModule = module {
    viewModel { (card: StoryCard) -> StoryCardViewModel() }
}

// :biz:story:message:presentation
val messagePresentationModule = module {
    viewModel { (reducer: Reducer<MessageState>) -> MessageViewModel(reducer) }
}

// :biz:story:infobar:presentation
val infoBarPresentationModule = module {
    viewModel { (reducer: Reducer<InfoBarState>) -> InfoBarViewModel(reducer) }
}

// :biz:story:input:presentation
val inputPresentationModule = module {
    viewModel { (reducer: Reducer<InputState>) -> InputViewModel(reducer) }
}

// :biz:story:background:presentation
val backgroundPresentationModule = module {
    viewModel { (reducer: Reducer<BackgroundState>) -> BackgroundViewModel(reducer) }
}
```

### 9.3 模块导出与 App 组装

```kotlin
// 各 biz 模块导出
val homeModules = listOf(homeDomainModule, homePresentationModule)
val feedModules = listOf(feedDomainModule, feedPresentationModule)
val storyModules = listOf(
    storyDomainModule, storyPresentationModule,
    messageDomainModule, messagePresentationModule,
    infoBarDomainModule, infoBarPresentationModule,
    inputDomainModule, inputPresentationModule,
    backgroundDomainModule, backgroundPresentationModule,
)

// :app 的 ComposeDemoApp.kt
startKoin {
    androidContext(this@ComposeDemoApp)
    modules(
        userCenterModule + storageModule +
        homeModules + feedModules + storyModules +
        loginModules + todoModules
    )
}
```

## 10. UI 层测试用例

| # | 测试名 | 预期行为 |
|---|--------|----------|
| 23 | `HomePage初始显示FeedScreen` | 默认 `selectedTab == HOME`，内容区渲染 FeedScreen |
| 24 | `HomePage切换Tab显示对应占位页` | 点击 DISCOVER 后内容区显示 `DiscoverPage`（文字"发现"） |
| 25 | `HomePage底部导航高亮当前Tab` | `selectedTab == MESSAGE` 时，MESSAGE 图标/文字处于高亮样式 |
| 26 | `HomePage角标正确渲染` | `tabBadges[MESSAGE]` 有未读数时，MESSAGE Tab 显示数字角标 |
| 27 | `FeedPage初始触发刷新` | `initEvent = OnRefresh`，FeedUseCase 的 `isRefreshing` 变为 true |
| 28 | `FeedPage上下滑动显示不同StoryCard` | VerticalPager 滑动到第 2 页，显示第 2 个 StoryCard 的背景图和文字 |
| 29 | `StoryCardPage背景图正确加载` | StoryCard 的 `backgroundImageUrl` 被 AsyncImage 加载并显示 |
| 30 | `StoryCardPage点击对白切换展开` | 点击 `MessageArea` 的对白，`isExpanded` 状态变化导致 UI 高度改变 |
| 31 | `StoryCardPage点击点赞更新图标和计数` | 点击 `InfoBarArea` 的点赞按钮，图标变为高亮，likes 数字变化 |
| 32 | `占位页面正确显示文本` | DiscoverPage 显示"发现"，MessagePage 显示"消息"，ProfilePage 显示"我的" |
