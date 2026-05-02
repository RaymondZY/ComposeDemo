package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

interface StateHolder<S> {
    val initialState: S
    val state: StateFlow<S>
    val currentState: S get() = state.value

    fun updateState(transform: (S) -> S)

    fun <D : UiState> derive(
        childSelector: (S) -> D,
        parentUpdater: S.(D) -> S,
    ): StateHolder<D> {
        return object : StateHolder<D> {
            override val initialState: D = childSelector(this@StateHolder.initialState)
            override val state: StateFlow<D> = DeriveStateFlow(this@StateHolder.state, childSelector)

            override fun updateState(transform: (D) -> D) {
                val currentParent = this@StateHolder.state.value
                val newChild = transform(childSelector(currentParent))
                val newParent = currentParent.parentUpdater(newChild)
                this@StateHolder.updateState { newParent }
            }
        }
    }
}

class StateHolderImpl<S : UiState>(
    override val initialState: S,
) : StateHolder<S> {
    private val _state = MutableStateFlow(initialState)

    override val state: StateFlow<S> = _state.asStateFlow()

    override fun updateState(transform: (S) -> S) {
        _state.update(transform)
    }
}

class DeriveStateFlow<P, C>(
    private val parent: StateFlow<P>,
    private val selector: (P) -> C,
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
