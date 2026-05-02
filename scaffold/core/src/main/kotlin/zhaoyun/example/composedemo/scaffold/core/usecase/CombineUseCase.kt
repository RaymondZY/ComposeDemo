package zhaoyun.example.composedemo.scaffold.core.usecase

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.EffectDispatcher
import zhaoyun.example.composedemo.scaffold.core.mvi.EventReceiver
import zhaoyun.example.composedemo.scaffold.core.mvi.EventReceiverImpl
import zhaoyun.example.composedemo.scaffold.core.mvi.MviFacade
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.autoRegister
import zhaoyun.example.composedemo.scaffold.core.spi.requireServiceRegistry

class CombineUseCase<S : UiState, E : UiEvent, F : UiEffect>(
    override val stateHolder: StateHolder<S>,
    vararg useCaseCreators: UseCaseFactory<S, E, F>,
) : MviFacade<S, E, F> {

    private val childUseCases = useCaseCreators.map { it(this.stateHolder) }

    init {
        autoRegister(requireServiceRegistry())
    }

    override val eventReceiver: EventReceiver<E> = EventReceiverImpl { event ->
        childUseCases.forEach { it.receiveEvent(event) }
    }

    override val effectDispatcher: EffectDispatcher<F> = CombineEffectDispatcher(childUseCases)

    internal fun allUseCases(): List<BaseUseCase<*, *, *>> = childUseCases

    @Deprecated(
        "attachParent is no longer needed. ServiceRegistry is now shared per-Screen via Koin Scope.",
        ReplaceWith("")
    )
    fun attachParent(serviceRegistry: ServiceRegistry) {
        // no-op: retained for binary compatibility during migration
    }
}

class CombineEffectDispatcher<F : UiEffect>(
    private val useCases: List<BaseUseCase<*, *, F>>,
) : EffectDispatcher<F> {

    private val ownEffect = Channel<F>(Channel.BUFFERED)

    private val ownBaseEffect = Channel<BaseEffect>(Channel.BUFFERED)

    override val effect = merge(
        *useCases.map { it.effect }.toTypedArray(),
        ownEffect.receiveAsFlow()
    )

    override val baseEffect = merge(
        *useCases.map { it.baseEffect }.toTypedArray(),
        ownBaseEffect.receiveAsFlow()
    )

    override fun dispatchEffect(effect: F) {
        ownEffect.trySend(effect)
    }

    override fun dispatchBaseEffect(baseEffect: BaseEffect) {
        ownBaseEffect.trySend(baseEffect)
    }
}
