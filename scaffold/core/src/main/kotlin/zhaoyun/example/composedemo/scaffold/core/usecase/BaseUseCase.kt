package zhaoyun.example.composedemo.scaffold.core.usecase

import zhaoyun.example.composedemo.scaffold.core.mvi.EffectDispatcher
import zhaoyun.example.composedemo.scaffold.core.mvi.EffectDispatcherImpl
import zhaoyun.example.composedemo.scaffold.core.mvi.EventReceiver
import zhaoyun.example.composedemo.scaffold.core.mvi.EventReceiverImpl
import zhaoyun.example.composedemo.scaffold.core.mvi.MviFacade
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolderImpl
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.find

typealias UseCaseFactory<S, E, F> = (StateHolder<S>) -> BaseUseCase<S, E, F>

abstract class BaseUseCase<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S,
    stateHolder: StateHolder<S>? = null,
) : MviFacade<S, E, F> {

    final override val stateHolder: StateHolder<S> = stateHolder ?: StateHolderImpl(initialState)

    final override val eventReceiver: EventReceiver<E> = EventReceiverImpl(::onEvent)

    final override val effectDispatcher: EffectDispatcher<F> = EffectDispatcherImpl()

    abstract suspend fun onEvent(event: E)

    protected var serviceRegistry: MutableServiceRegistry? = null

    fun attachServiceRegistry(registry: MutableServiceRegistry) {
        this.serviceRegistry = registry
    }

    fun detachServiceRegistry() {
        this.serviceRegistry = null
    }

    protected inline fun <reified T : Any> findService(tag: String? = null): T {
        return serviceRegistry?.find<T>(tag)
            ?: error(
                "Service ${T::class.java.name} not found in current scope. " +
                        "Did you forget to register it from a ServiceProvider or implement UseCaseService?"
            )
    }

    /**
     * 在所在作用域内查找服务实现。
     * 找不到时返回 null，不抛异常。
     */
    protected inline fun <reified T : Any> findServiceOrNull(tag: String? = null): T? {
        return serviceRegistry?.find<T>(tag)
    }

    protected inline fun <reified T : Any> registerService(
        instance: T,
        tag: String? = null,
    ) {
        checkNotNull(serviceRegistry) {
            "No MutableServiceRegistry is attached to ${javaClass.name}"
        }.register(T::class.java, instance, tag)
    }

    protected inline fun <reified T : Any> unregisterService(tag: String? = null) {
        serviceRegistry?.unregister(T::class.java, tag)
    }

    protected fun unregisterService(instance: Any) {
        serviceRegistry?.unregister(instance)
    }
}
