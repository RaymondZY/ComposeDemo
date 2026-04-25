package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * 从父 [StateFlow] 派生的只读子状态流。
 *
 * [value] 实时计算，不缓存旧值；[collect] 委托给父流的 map+distinctUntilChanged。
 * 这种实现避免了 stateIn + backgroundScope 在 runTest 中的时序问题。
 */
private class DerivedStateFlow<P, C>(
    private val parent: StateFlow<P>,
    private val selector: (P) -> C
) : StateFlow<C> {
    override val value: C
        get() = selector(parent.value)

    override val replayCache: List<C>
        get() = listOf(value)

    override suspend fun collect(collector: FlowCollector<C>): Nothing {
        parent.map { selector(it) }.distinctUntilChanged().collect(collector)
        throw IllegalStateException("DerivedStateFlow.collect should never complete")
    }
}

/**
 * 在 [StateHolder] 上创建子状态代理，支持多级嵌套状态切片。
 *
 * 该扩展函数允许从父状态中切出子状态，形成双向同步：
 * - 父状态变化 → 子状态通过 [DerivedStateFlow] 自动派生（value 实时计算）
 * - 子状态更新 → 通过 [updater] 写回父状态
 *
 * ## 多级嵌套
 * 子 [StateHolder] 可以继续调用 [createChild]，实现孙状态代理：
 * ```
 * val grandChildHolder = childHolder.createChild(scope, selector, updater)
 * ```
 *
 * @param scope 保留参数以兼容现有 API（内部不再启动协程）
 * @param selector 从父状态中选择子状态的函数（只做 O(1) 属性访问）
 * @param updater 将新子状态合并回父状态的函数
 * @param started 保留参数以兼容现有 API
 */
fun <P, C> StateHolder<P>.createChild(
    scope: kotlinx.coroutines.CoroutineScope,
    selector: (P) -> C,
    updater: (P, C) -> P,
    started: kotlinx.coroutines.flow.SharingStarted = kotlinx.coroutines.flow.SharingStarted.Eagerly,
): StateHolder<C> {
    val childFlow = DerivedStateFlow(state, selector)

    return DelegateStateHolder(
        state = childFlow,
        onUpdate = { transform ->
            val currentParent = state.value
            val newChild = transform(selector(currentParent))
            val newParent = updater(currentParent, newChild)
            update { newParent }
        }
    )
}
