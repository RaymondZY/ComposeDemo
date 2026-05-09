# DI 配置说明

> 本文档说明 ComposeDemo 项目中依赖注入的配置方式。项目模块分层与整体架构见 [overview.md](./overview.md)，MVI 框架实现见 [mvi.md](./mvi.md)。

---

## 1. Koin 初始化

所有 Koin 模块在 `:app` 的 `ComposeDemoApp.onCreate()` 中统一注册：

```kotlin
// app/src/main/java/.../ComposeDemoApp.kt
startKoin {
    androidContext(this@ComposeDemoApp)
    modules(
        userCenterModule + storageModule +
            homeModules + feedModules +
            listOf(
                storyPresentationModule,
                messagePresentationModule,
                infoBarPresentationModule,
                commentPanelPresentationModule,
                sharePanelPresentationModule,
                inputPresentationModule,
                backgroundPresentationModule,
                storyPanelPresentationModule,
            ) +
            listOf(
                module {
                    single { FakeFeedRepository() } bind FeedRepository::class
                    single { InputKeyboardCoordinator() }
                },
            )
    )
}
```

---

## 2. 带参数的 ViewModel Factory

需要注入 `StateHolder` 的子 ViewModel 使用带参数的 factory：

```kotlin
// biz/story/message/presentation/di/MessagePresentationModule.kt
val messagePresentationModule = module {
    scope(MviKoinScopes.Item) {
        viewModel { (stateHolder: StateHolder<MessageState>) ->
            MessageViewModel(
                stateHolder = stateHolder,
                serviceRegistry = get(),
            )
        }
    }
}
```

注入方式：

```kotlin
val messageViewModel: MessageViewModel = screenViewModel(card.cardId) {
    parametersOf(storyViewModel.messageStateHolder)
}
```

`MutableServiceRegistry` 不再由调用方通过 `parametersOf` 传入，而是由当前 Koin Scope 通过 `get()` 解析。

---

## 3. 作用域与 Registry 的创建

`MviScreen` 和 `MviItemScope` 在创建时会同时建立独立的 Koin Scope 和 `MutableServiceRegistry`：

- **Koin Scope**：隔离不同 Screen / 列表项的 ViewModel 实例，避免全局单例冲突
- **ServiceRegistry**：管理当前作用域内的 UseCase 服务注册，支持 parent chain 查找（详见 [mvi.md §7](./mvi.md)）

```kotlin
// MviScreen 内部伪代码
val scope = koin.createScope(scopeId, MviKoinScopes.Screen)
val registry = MutableServiceRegistryImpl(parent = koinRegistry)
scope.declare<MutableServiceRegistryImpl>(
    registry,
    secondaryTypes = listOf(ServiceRegistry::class, MutableServiceRegistry::class),
)
```

---

## 4. 模块注册清单（示例）

| 模块                                      | Koin Module                      | 说明                                      |
|-----------------------------------------|----------------------------------|-----------------------------------------|
| `:biz:story:presentation`               | `storyPresentationModule`        | StoryCard 页面 ViewModel                  |
| `:biz:story:message:presentation`       | `messagePresentationModule`      | Message 子组件 ViewModel（带 StateHolder 参数） |
| `:biz:story:infobar:presentation`       | `infoBarPresentationModule`      | InfoBar 子组件 ViewModel                   |
| `:biz:story:input:presentation`         | `inputPresentationModule`        | Input 子组件 ViewModel                     |
| `:biz:story:background:presentation`    | `backgroundPresentationModule`   | Background 子组件 ViewModel                |
| `:biz:story:comment-panel:presentation` | `commentPanelPresentationModule` | CommentPanel 底部面板 ViewModel             |
| `:biz:story:share-panel:presentation`   | `sharePanelPresentationModule`   | SharePanel 底部面板 ViewModel               |
| `:biz:story:story-panel:presentation`   | `storyPanelPresentationModule`   | StoryPanel 详情占位页面 ViewModel             |
| `:biz:feed:presentation`                | `feedModules`                    | Feed 页面相关模块                             |
| `:biz:home:presentation`                | `homeModules`                    | Home 页面相关模块                             |
| `:service:user-center`                  | `userCenterModule`               | 用户中心服务                                  |
| `:service:storage`                      | `storageModule`                  | 存储服务                                    |
| `:service:feed:mock`                    | `FakeFeedRepository`             | FeedRepository mock 实现                  |
| `:biz:story:input:domain`               | `InputKeyboardCoordinator`       | 输入框焦点与键盘协调服务                            |
