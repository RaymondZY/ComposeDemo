package zhaoyun.example.composedemo.scaffold.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import zhaoyun.example.composedemo.scaffold.core.usecase.CombineUseCase
import zhaoyun.example.composedemo.scaffold.core.mvi.EffectDispatcher
import zhaoyun.example.composedemo.scaffold.core.mvi.EventReceiver
import zhaoyun.example.composedemo.scaffold.core.mvi.MviFacade
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.UseCaseFactory

open class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S,
    vararg useCases: UseCaseFactory<S, E, F>,
    stateHolder: StateHolder<S>? = null,
    parentServiceRegistry: ServiceRegistry? = null,
) : ViewModel(),
    MviFacade<S, E, F> {

    val serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(parent = parentServiceRegistry)

    private val combineUseCase = CombineUseCase(
        initialState,
        *useCases,
        stateHolder = stateHolder,
        serviceRegistry = serviceRegistry,
    )

    override val stateHolder: StateHolder<S>
        get() = combineUseCase.stateHolder

    override val eventReceiver: EventReceiver<E>
        get() = combineUseCase.eventReceiver

    override val effectDispatcher: EffectDispatcher<F>
        get() = combineUseCase.effectDispatcher

    fun sendEvent(event: E) {
        viewModelScope.launch {
            receiveEvent(event)
        }
    }

    fun <T : Any> registerService(
        clazz: Class<T>,
        instance: T,
        tag: String? = null,
    ) {
        serviceRegistry.register(clazz, instance, tag)
    }

    inline fun <reified T : Any> registerService(
        instance: T,
        tag: String? = null,
    ) {
        serviceRegistry.register(T::class.java, instance, tag)
    }

    fun unregisterService(
        clazz: Class<*>,
        tag: String? = null,
    ) {
        serviceRegistry.unregister(clazz, tag)
    }

    fun unregisterService(instance: Any) {
        serviceRegistry.unregister(instance)
    }

    override fun onCleared() {
        serviceRegistry.clear()
        super.onCleared()
    }
}
