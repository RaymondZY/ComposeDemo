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

    // Cache the Screen-level registry during init so onCleared() can unregister without Stack
    @PublishedApi
    internal val screenRegistry: MutableServiceRegistry = requireServiceRegistry()

    @Deprecated(
        "Accessing serviceRegistry directly is discouraged. Use registerService/unregisterService or the shared Screen registry.",
        ReplaceWith("")
    )
    val serviceRegistry: MutableServiceRegistry get() = screenRegistry

    init {
        autoRegister(screenRegistry)
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
        screenRegistry.register(clazz, instance, tag)
    }

    inline fun <reified T : Any> registerService(
        instance: T,
        tag: String? = null,
    ) {
        screenRegistry.register(T::class.java, instance, tag)
    }

    fun unregisterService(
        clazz: Class<*>,
        tag: String? = null,
    ) {
        screenRegistry.unregister(clazz, tag)
    }

    fun unregisterService(instance: Any) {
        screenRegistry.unregister(instance)
    }

    override fun onCleared() {
        // Unregister the whole tree: children → combine → self
        combineUseCase.allUseCases().forEach { it.autoUnregister(screenRegistry) }
        combineUseCase.autoUnregister(screenRegistry)
        autoUnregister(screenRegistry)
        super.onCleared()
    }
}
