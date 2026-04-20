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
    private var _externalState: MutableStateFlow<S>? = null

    private val activeState: MutableStateFlow<S>
        get() = _externalState ?: _internalState

    val state: StateFlow<S> get() = activeState.asStateFlow()

    private val _effect = Channel<F>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val _baseEffect = Channel<BaseEffect>(Channel.BUFFERED)
    val baseEffect = _baseEffect.receiveAsFlow()

    protected val currentState: S get() = activeState.value

    protected fun updateState(transform: (S) -> S) {
        activeState.update(transform)
    }

    protected fun sendEffect(effect: F) {
        _effect.trySend(effect)
    }

    protected fun sendBaseEffect(effect: BaseEffect) {
        _baseEffect.trySend(effect)
    }

    /**
     * 将该 UseCase 绑定到外部共享的 StateFlow（通常由 ViewModel 提供）。
     * 绑定后，所有状态读写操作都会路由到 [sharedState]，实现多个 UseCase 共享同一份 State。
     */
    fun bind(sharedState: MutableStateFlow<S>) {
        _externalState = sharedState
    }

    abstract suspend fun onEvent(event: E)
}
