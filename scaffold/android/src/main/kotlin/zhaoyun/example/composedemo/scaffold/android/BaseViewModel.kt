package zhaoyun.example.composedemo.scaffold.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase
import zhaoyun.example.composedemo.scaffold.core.mvi.DelegateReducer
import zhaoyun.example.composedemo.scaffold.core.mvi.LocalReducer
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer
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
 * - 默认情况下，内部使用 [LocalReducer]，ViewModel 独立管理自己的状态
 * - 可通过构造函数注入外部 [Reducer]（如 [DelegateReducer]），实现状态代理到 GlobalViewModel
 * - 通过 [createDelegateReducer] 从父 State 中切片出子 Reducer，实现嵌套状态共享
 *
 * ## 初始化时序说明
 * Reducer 在构造函数中直接创建，UseCase 在 `init` 块中立即绑定到 Reducer，
 * 无需延迟初始化。
 */
abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S,
    injectedReducer: Reducer<S>? = null,
    private vararg val useCases: BaseUseCase<S, E, F>
) : ViewModel() {

    private val reducer: Reducer<S> = injectedReducer ?: LocalReducer(initialState)
    val state: StateFlow<S> = reducer.state
    val effect: Flow<F> = merge(*useCases.map { it.effect }.toTypedArray())
    val baseEffect: Flow<BaseEffect> = merge(*useCases.map { it.baseEffect }.toTypedArray())

    init {
        useCases.forEach { it.bind(reducer) }
    }

    fun onEvent(event: E) {
        viewModelScope.launch {
            useCases.forEach { it.onEvent(event) }
        }
    }

    protected fun updateState(transform: (S) -> S) {
        reducer.reduce(transform)
    }

    fun <T> createDelegateReducer(childSelector: (S) -> T, parentUpdater: (S, T) -> S): Reducer<T> {
        val childStateFlow = MutableStateFlow(childSelector(state.value))
        return DelegateReducer(
            state = childStateFlow,
            onReduce = { transform ->
                val childNewState = transform(childSelector(state.value))
                childStateFlow.value = childNewState

                val parentNewState = parentUpdater(state.value, childNewState)
                updateState { parentNewState }
            }
        )
    }
}
