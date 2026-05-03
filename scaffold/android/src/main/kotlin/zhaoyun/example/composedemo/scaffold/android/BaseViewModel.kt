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
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistryAccessor
import zhaoyun.example.composedemo.scaffold.core.spi.autoRegister
import zhaoyun.example.composedemo.scaffold.core.spi.autoUnregister
import zhaoyun.example.composedemo.scaffold.core.usecase.CombineUseCase
import zhaoyun.example.composedemo.scaffold.core.usecase.UseCaseFactory

open class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    override val stateHolder: StateHolder<S>,
    override val serviceRegistry: MutableServiceRegistry,
    vararg useCaseCreators: UseCaseFactory<S, E, F>,
) : ViewModel(), MviFacade<S, E, F>, ServiceRegistryAccessor {

    private val combineUseCase = CombineUseCase(stateHolder, serviceRegistry, *useCaseCreators)

    init {
        autoRegister(serviceRegistry)
    }

    override val eventReceiver: EventReceiver<E> get() = combineUseCase.eventReceiver
    override val effectDispatcher: EffectDispatcher<F> get() = combineUseCase.effectDispatcher

    fun sendEvent(event: E) {
        viewModelScope.launch {
            receiveEvent(event)
        }
    }

    override fun onCleared() {
        combineUseCase.onCleared()
        this.autoUnregister(serviceRegistry)
        super.onCleared()
    }
}
