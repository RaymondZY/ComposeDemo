package zhaoyun.example.composedemo.scaffold.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
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
 * - 默认情况下，[createReducer] 返回 [LocalReducer]，ViewModel 独立管理自己的状态
 * - 子类可通过覆盖 [createReducer] 注入外部 [Reducer]，实现状态代理到 GlobalViewModel
 * - 装配层通过 [createDelegateReducer] 创建代理 Reducer，实现 Detail 的无感知嵌入
 *
 * ## 初始化时序说明
 * 为避免在 `init` 块中调用 `open` 方法导致子类属性尚未初始化的问题，
 * Reducer 的创建与 UseCase 的绑定采用 **延迟初始化** 策略：
 * 首次访问 [state] 或首次调用 [onEvent] 时，才会真正创建 Reducer 并绑定所有 UseCase。
 */
abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    private val initialState: S,
    private vararg val useCases: BaseUseCase<S, E, F>,
    private val injectedReducer: Reducer<S>? = null
) : ViewModel() {

    /**
     * 创建该 ViewModel 使用的 [Reducer]。
     * 子类可覆盖此方法以注入外部 [Reducer]（如 [DelegateReducer]），实现状态代理。
     *
     * **注意**：此方法可能在子类构造函数完成之前被调用，
     * 因此覆盖实现中不应访问子类中定义的属性（除非有默认值或惰性初始化）。
     */
    protected open fun createReducer(initialState: S): Reducer<S> = LocalReducer(initialState)

    private var _reducer: Reducer<S>? = null
    private var isBound = false

    private fun ensureReducer(): Reducer<S> {
        if (_reducer == null) {
            _reducer = createReducer(initialState)
        }
        return _reducer!!
    }

    private fun ensureBound() {
        if (!isBound) {
            val reducer = ensureReducer()
            useCases.forEach { it.bind(reducer) }
            isBound = true
        }
    }

    val state: StateFlow<S> by lazy {
        ensureBound()
        ensureReducer().state
    }

    val effect = merge(*useCases.map { it.effect }.toTypedArray())
    val baseEffect = merge(*useCases.map { it.baseEffect }.toTypedArray())

    fun onEvent(event: E) {
        ensureBound()
        viewModelScope.launch {
            useCases.forEach { it.onEvent(event) }
        }
    }

    companion object {
        /**
         * 创建代理 [Reducer]，供 GlobalViewModel 注入给 Detail ViewModel 使用。
         *
         * @param stateFlow 外部提供的 StateFlow（通常是从 GlobalState 中切片的 Flow）
         * @param onReduce 状态更新回调，所有 Detail 的 reduce 操作都会经过此回调写回外部状态
         */
        fun <S> createDelegateReducer(
            stateFlow: StateFlow<S>,
            onReduce: ((S) -> S) -> Unit
        ): Reducer<S> = DelegateReducer(stateFlow, onReduce)
    }
}
