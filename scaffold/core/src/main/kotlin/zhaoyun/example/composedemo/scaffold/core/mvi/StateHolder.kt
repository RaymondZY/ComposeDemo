package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 状态读写最小契约 —— 决定「状态存在哪里」以及「如何更新它」
 *
 * [BaseViewModel] 与 [BaseUseCase] 均面向此接口编程，
 * 从而实现「独立持态」与「代理到外部」两种模式的无感知切换。
 */
interface StateHolder<S> {
    val state: StateFlow<S>
    fun update(transform: (S) -> S)
}

/**
 * 本地 StateHolder —— 内部持有 [MutableStateFlow]，作为独立页面时的默认实现
 */
class LocalStateHolder<S>(initial: S) : StateHolder<S> {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<S> = _state.asStateFlow()
    override fun update(transform: (S) -> S) {
        _state.update(transform)
    }
}

/**
 * 代理 StateHolder —— 将状态读写代理到外部提供的 [StateFlow] 与 [onUpdate] 回调
 *
 * 典型使用场景：直接创建 [DelegateStateHolder] 实例注入给 Detail ViewModel，
 * 使 DetailState 成为 GlobalState 的结构性子集。
 */
class DelegateStateHolder<S>(
    override val state: StateFlow<S>,
    private val onUpdate: ((S) -> S) -> Unit
) : StateHolder<S> {
    override fun update(transform: (S) -> S) = onUpdate(transform)
}
