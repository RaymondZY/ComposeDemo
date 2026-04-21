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
interface Reducer<S> {
    val state: StateFlow<S>
    fun reduce(transform: (S) -> S)
}

/**
 * 本地 Reducer —— 内部持有 [MutableStateFlow]，作为独立页面时的默认实现
 */
class LocalReducer<S>(initial: S) : Reducer<S> {
    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<S> = _state.asStateFlow()
    override fun reduce(transform: (S) -> S) {
        _state.update(transform)
    }
}

/**
 * 代理 Reducer —— 将状态读写代理到外部提供的 [StateFlow] 与 [onReduce] 回调
 *
 * 典型使用场景：GlobalViewModel 通过 [BaseViewModel.createDelegateReducer] 创建实例，
 * 注入给 Detail ViewModel，使 DetailState 成为 GlobalState 的结构性子集。
 */
class DelegateReducer<S>(
    override val state: StateFlow<S>,
    private val onReduce: ((S) -> S) -> Unit
) : Reducer<S> {
    override fun reduce(transform: (S) -> S) = onReduce(transform)
}
