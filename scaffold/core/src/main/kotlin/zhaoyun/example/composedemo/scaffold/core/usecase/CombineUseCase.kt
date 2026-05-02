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
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.MviService
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.TaggedMviService

class CombineUseCase<S : UiState, E : UiEvent, F : UiEffect>(
    override val stateHolder: StateHolder<S>,
    vararg useCaseCreators: UseCaseFactory<S, E, F>,
) : MviFacade<S, E, F> {

    private val serviceRegistry = MutableServiceRegistryImpl()

    private val childUseCases = useCaseCreators.map { it(this.stateHolder) }.onEach { useCase ->
        useCase.attachParent(serviceRegistry)
        registerAutoServices(useCase, serviceRegistry)
    }

    override val eventReceiver: EventReceiver<E> = EventReceiverImpl { event ->
        childUseCases.forEach { it.receiveEvent(event) }
    }

    override val effectDispatcher: EffectDispatcher<F> = CombineEffectDispatcher(childUseCases)

    fun attachParent(serviceRegistry: ServiceRegistry) {
        this.serviceRegistry.attachParent(serviceRegistry)
    }
}

private fun registerAutoServices(
    useCase: BaseUseCase<*, *, *>,
    registry: MutableServiceRegistry,
) {
    val tag = (useCase as? TaggedMviService)?.serviceTag
    collectAutoServiceTypes(useCase.javaClass).forEach { serviceType ->
        @Suppress("UNCHECKED_CAST")
        registry.register(serviceType as Class<Any>, useCase, tag)
    }
}

private fun collectAutoServiceTypes(clazz: Class<*>): Set<Class<*>> {
    val discovered = linkedSetOf<Class<*>>()
    val pending = ArrayDeque<Class<*>>()
    pending += clazz

    while (pending.isNotEmpty()) {
        val current = pending.removeFirst()
        current.superclass?.let { pending += it }
        current.interfaces.forEach { interfaceType ->
            pending += interfaceType
            if (
                interfaceType != MviService::class.java &&
                interfaceType != TaggedMviService::class.java &&
                MviService::class.java.isAssignableFrom(interfaceType)
            ) {
                discovered += interfaceType
            }
        }
    }

    return discovered
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
