# Reducer → StateHolder 全局重命名设计文档

## 背景

当前 MVI 框架中，负责状态读写最小契约的接口名为 `Reducer`，其更新方法为 `reduce()`。该命名对框架内部人员尚可理解，但对新接触者不够直观——"Reducer" 带有 Redux/函数式编程的特定语义，而这里实际只是一个「持有状态并提供更新能力」的组件。将其重命名为 `StateHolder`，方法改为 `update()`，可显著降低认知成本。

## 目标

将项目中所有与 `Reducer` 相关的命名（类名、方法名、变量名、参数名、注释、文档）统一替换为 `StateHolder` / `update`，不留废弃别名，不引入逻辑变更。

## 命名映射表

| 旧命名 | 新命名 | 类型 | 说明 |
|--------|--------|------|------|
| `Reducer` | `StateHolder` | `interface` | 核心状态契约接口 |
| `LocalReducer` | `LocalStateHolder` | `class` | 内部持有 `MutableStateFlow` 的默认实现 |
| `DelegateReducer` | `DelegateStateHolder` | `class` | 将状态读写代理到外部的实现 |
| `reduce()` | `update()` | 方法 | 状态更新原子操作 |
| `reducer` | `stateHolder` | 变量/参数 | 泛指接口实例的命名 |
| `injectedReducer` | `injectedStateHolder` | 参数 | `BaseViewModel` 构造参数 |
| `_reducer` | `_stateHolder` | 私有属性 | `BaseUseCase` 内部持有的绑定对象 |
| `onReduce` | `onUpdate` | Lambda 参数 | `DelegateStateHolder` 的回调参数名 |
| `createDelegateReducer` | `createDelegateStateHolder` | 方法 | `BaseViewModel` 创建子状态持有者的方法 |
| `Reducer.kt` | `StateHolder.kt` | 文件名 | `:scaffold:core` 中的源文件 |

## 影响范围

### `:scaffold:core`
- `mvi/Reducer.kt` → 重命名为 `mvi/StateHolder.kt`
- `mvi/BaseUseCase.kt` — 修改内部属性、方法参数、KDoc
- `mvi/ReducerTest.kt` — 重命名为 `StateHolderTest.kt`，修改所有引用
- `mvi/BaseUseCaseReducerBindTest.kt` — 重命名为 `BaseUseCaseStateHolderBindTest.kt`
- `mvi/GlobalReducerIntegrationTest.kt` — 重命名为 `GlobalStateHolderIntegrationTest.kt`

### `:scaffold:android`
- `android/BaseViewModel.kt` — 修改导入、参数、方法、KDoc
- `android/MviScreenReducerIntegrationTest.kt` — 重命名为 `MviScreenStateHolderIntegrationTest.kt`

### 业务模块 (`:biz:*`)
所有使用 `Reducer`、`LocalReducer`、`DelegateReducer`、`createDelegateReducer`、`reduce()` 的以下类型文件：
- `*ViewModel.kt` — ViewModel 类定义、注入的 reducer 参数
- `*Module.kt` / `di/*Module.kt` — Koin DI 模块中的参数类型
- `*Page.kt` / `*Screen.kt` — Composable 中调用 `createDelegateReducer` 或注入 reducer 的地方

涉及模块：
- `:biz:feed:presentation`
- `:biz:home:presentation`
- `:biz:login:presentation`
- `:biz:story:presentation` 及其子模块（message、input、infobar、background）
- `:biz:todo-list:presentation`

### 文档
- `MVI_FRAMEWORK.md` — 全文替换
- `docs/superpowers/specs/2026-04-24-feed-home-design.md` — 涉及引用处替换
- `docs/superpowers/plans/2026-04-24-feed-home-plan.md` — 涉及引用处替换

## 实现策略

采用**一次性全量替换**：
1. 使用全局搜索确定所有引用点
2. 按模块批次修改文件（core → android → biz）
3. 每批次后执行 `./gradlew compileDebugKotlin` 验证编译
4. 全部修改完毕后执行 `./gradlew test` 验证测试
5. 如有 Android 测试失败，针对性修复

## 验证标准

- [ ] `./gradlew compileDebugKotlin` 全量编译通过，零报错
- [ ] `./gradlew test` 所有单元测试通过
- [ ] 代码中不再出现 `Reducer`、`LocalReducer`、`DelegateReducer`、`reduce(`、`createDelegateReducer` 等旧命名（注释与字符串常量除外，如历史提交信息）
- [ ] `MVI_FRAMEWORK.md` 中所有示例代码与说明文字已同步更新

## 非目标（YAGNI）

- 不修改 `updateState` 方法名（`BaseUseCase` / `BaseViewModel` 中的 `updateState` 保持不变，它内部调用 `stateHolder.update()`）
- 不修改 `UiState` / `UiEvent` / `UiEffect` 等三要素命名
- 不引入新功能或逻辑变更
- 不保留 `@Deprecated` 别名
