package zhaoyun.example.composedemo.scaffold.core.usecase

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
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.autoRegister
import zhaoyun.example.composedemo.scaffold.core.spi.autoUnregister
import zhaoyun.example.composedemo.scaffold.core.spi.find
import zhaoyun.example.composedemo.scaffold.core.spi.requireServiceRegistry

typealias UseCaseFactory<S, E, F> = (StateHolder<S>) -> BaseUseCase<S, E, F>

abstract class BaseUseCase<S : UiState, E : UiEvent, F : UiEffect>(
    override val stateHolder: StateHolder<S>,
) : MviFacade<S, E, F> {

    final override val eventReceiver: EventReceiver<E> = EventReceiverImpl(::onEvent)

    final override val effectDispatcher: EffectDispatcher<F> = EffectDispatcherImpl()

    abstract suspend fun onEvent(event: E)

    init {
        autoRegister(requireServiceRegistry())
    }

    @Deprecated(
        "attachParent is no longer needed. ServiceRegistry is now shared per-Screen via Koin Scope.",
        ReplaceWith("")
    )
    fun attachParent(registry: ServiceRegistry) {
        // no-op: retained for binary compatibility during migration
    }

    @Deprecated(
        "detachParent is no longer needed.",
        ReplaceWith("")
    )
    fun detachParent() {
        // no-op
    }

    protected inline fun <reified T : Any> findService(tag: String? = null): T {
        return requireServiceRegistry().find<T>(tag)
            ?: error(
                "Service ${T::class.java.name} not found in current scope. " +
                    "Did you forget to register it from a ServiceProvider or auto-expose an MviService?"
            )
    }

    protected inline fun <reified T : Any> findServiceOrNull(tag: String? = null): T? {
        return requireServiceRegistry().find<T>(tag)
    }

    protected inline fun <reified T : Any> registerService(
        instance: T,
        tag: String? = null,
    ) {
        requireServiceRegistry().register(T::class.java, instance, tag)
    }

    protected inline fun <reified T : Any> unregisterService(tag: String? = null) {
        requireServiceRegistry().unregister(T::class.java, tag)
    }

    protected fun unregisterService(instance: Any) {
        requireServiceRegistry().unregister(instance)
    }
}
