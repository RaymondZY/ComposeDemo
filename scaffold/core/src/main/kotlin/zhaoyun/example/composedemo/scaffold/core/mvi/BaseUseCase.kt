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
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    private val _effect = Channel<F>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private val _baseEffect = Channel<BaseEffect>(Channel.BUFFERED)
    val baseEffect = _baseEffect.receiveAsFlow()

    protected val currentState: S get() = _state.value

    protected fun updateState(transform: (S) -> S) {
        _state.update(transform)
    }

    protected fun sendEffect(effect: F) {
        _effect.trySend(effect)
    }

    protected fun sendBaseEffect(effect: BaseEffect) {
        _baseEffect.trySend(effect)
    }

    abstract suspend fun onEvent(event: E)
}
