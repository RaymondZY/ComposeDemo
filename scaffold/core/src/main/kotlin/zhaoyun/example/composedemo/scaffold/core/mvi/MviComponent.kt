package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MviComponent<S : UiState, E : UiEvent, F : UiEffect> {
    val stateHolder: StateHolder<S>
    val eventReceiver: EventReceiver<E>
    val effectDispatcher: EffectDispatcher<F>
}

interface MviFacade<S : UiState, E : UiEvent, F : UiEffect> :
    MviComponent<S, E, F>,
    StateHolder<S>,
    EventReceiver<E>,
    EffectDispatcher<F> {

    override val initialState: S
        get() = stateHolder.initialState

    override val state: StateFlow<S>
        get() = stateHolder.state

    override val effect: Flow<F>
        get() = effectDispatcher.effect

    override val baseEffect: Flow<BaseEffect>
        get() = effectDispatcher.baseEffect

    override fun updateState(transform: (S) -> S) {
        stateHolder.updateState(transform)
    }

    override suspend fun receiveEvent(event: E) {
        eventReceiver.receiveEvent(event)
    }

    override fun dispatchEffect(effect: F) {
        effectDispatcher.dispatchEffect(effect)
    }

    override fun dispatchBaseEffect(baseEffect: BaseEffect) {
        effectDispatcher.dispatchBaseEffect(baseEffect)
    }
}
