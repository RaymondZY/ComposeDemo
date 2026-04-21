package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

abstract class BaseUseCase<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S
) {
    private val _internalState = MutableStateFlow(initialState)
    private var _reducer: Reducer<S>? = null

    private val activeState: StateFlow<S>
        get() = _reducer?.state ?: _internalState.asStateFlow()

    val state: StateFlow<S> get() = activeState

    private val _effect = Channel<F>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val _baseEffect = Channel<BaseEffect>(Channel.BUFFERED)
    val baseEffect = _baseEffect.receiveAsFlow()

    protected val currentState: S get() = activeState.value

    protected fun updateState(transform: (S) -> S) {
        _reducer?.reduce(transform) ?: _internalState.update(transform)
    }

    protected fun sendEffect(effect: F) {
        _effect.trySend(effect)
    }

    protected fun sendBaseEffect(effect: BaseEffect) {
        _baseEffect.trySend(effect)
    }

    /**
     * 将该 UseCase 绑定到外部 [Reducer]（通常由 [BaseViewModel] 提供）。
     * 绑定后，所有状态读写操作都会路由到 [reducer]，实现多个 UseCase 共享同一份 State。
     */
    fun bind(reducer: Reducer<S>) {
        _reducer = reducer
    }

    abstract suspend fun onEvent(event: E)
}
