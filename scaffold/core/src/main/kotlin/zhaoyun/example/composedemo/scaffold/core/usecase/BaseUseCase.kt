package zhaoyun.example.composedemo.scaffold.core.usecase

import zhaoyun.example.composedemo.scaffold.core.context.MviContext
import zhaoyun.example.composedemo.scaffold.core.mvi.EffectDispatcher
import zhaoyun.example.composedemo.scaffold.core.mvi.EffectDispatcherImpl
import zhaoyun.example.composedemo.scaffold.core.mvi.EventReceiver
import zhaoyun.example.composedemo.scaffold.core.mvi.EventReceiverImpl
import zhaoyun.example.composedemo.scaffold.core.mvi.MviFacade
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry

typealias UseCaseFactory<S, E, F> = (StateHolder<S>, MutableServiceRegistry) -> BaseUseCase<S, E, F>

abstract class BaseUseCase<S : UiState, E : UiEvent, F : UiEffect>(
    override val stateHolder: StateHolder<S>,
    override val serviceRegistry: MutableServiceRegistry,
) : MviFacade<S, E, F>, MviContext {

    init {
        logger().i("Mvi", "Created ${this::class.simpleName}")
    }

    final override val eventReceiver: EventReceiver<E> = EventReceiverImpl(::onEvent)

    final override val effectDispatcher: EffectDispatcher<F> = EffectDispatcherImpl()

    abstract suspend fun onEvent(event: E)

    open fun onCleared() = Unit
}
