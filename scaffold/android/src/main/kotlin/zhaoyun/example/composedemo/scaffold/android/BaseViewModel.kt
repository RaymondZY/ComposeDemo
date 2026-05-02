package zhaoyun.example.composedemo.scaffold.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import zhaoyun.example.composedemo.scaffold.core.mvi.EffectDispatcher
import zhaoyun.example.composedemo.scaffold.core.mvi.EventReceiver
import zhaoyun.example.composedemo.scaffold.core.mvi.MviFacade
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.autoRegister
import zhaoyun.example.composedemo.scaffold.core.spi.autoUnregister
import zhaoyun.example.composedemo.scaffold.core.spi.requireServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.CombineUseCase
import zhaoyun.example.composedemo.scaffold.core.usecase.UseCaseFactory

open class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    override val stateHolder: StateHolder<S>,
    vararg useCaseCreators: UseCaseFactory<S, E, F>,
) : ViewModel(),
    MviFacade<S, E, F> {

    private val combineUseCase = CombineUseCase(
        stateHolder = stateHolder,
        useCaseCreators = useCaseCreators
    )

    private var screenRegistry: MutableServiceRegistry? = null

    init {
        screenRegistry = runCatching { requireServiceRegistry() }.getOrNull()
        screenRegistry?.let { autoRegister(it) }
    }

    internal fun ensureRegistered(registry: MutableServiceRegistry) {
        if (screenRegistry != null) return
        screenRegistry = registry
        combineUseCase.allUseCases().forEach { it.autoRegister(registry) }
        combineUseCase.autoRegister(registry)
        autoRegister(registry)
    }

    override val eventReceiver: EventReceiver<E>
        get() = combineUseCase.eventReceiver

    override val effectDispatcher: EffectDispatcher<F>
        get() = combineUseCase.effectDispatcher

    fun sendEvent(event: E) {
        viewModelScope.launch {
            receiveEvent(event)
        }
    }

    @Deprecated(
        "attachParent is no longer needed. ServiceRegistry is now shared per-Screen via Koin Scope.",
        ReplaceWith("")
    )
    fun attachParent(serviceRegistry: ServiceRegistry) {
        // no-op: retained for binary compatibility during migration
    }

    fun <T : Any> registerService(
        clazz: Class<T>,
        instance: T,
        tag: String? = null,
    ) {
        (screenRegistry ?: error("No screen registry available"))
            .register(clazz, instance, tag)
    }

    inline fun <reified T : Any> registerService(
        instance: T,
        tag: String? = null,
    ) {
        registerService(T::class.java, instance, tag)
    }

    fun unregisterService(
        clazz: Class<*>,
        tag: String? = null,
    ) {
        screenRegistry?.unregister(clazz, tag)
    }

    fun unregisterService(instance: Any) {
        screenRegistry?.unregister(instance)
    }

    override fun onCleared() {
        screenRegistry?.let { registry ->
            combineUseCase.allUseCases().forEach { it.autoUnregister(registry) }
            combineUseCase.autoUnregister(registry)
            autoUnregister(registry)
        }
        super.onCleared()
    }
}
