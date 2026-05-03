package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase
import zhaoyun.example.composedemo.scaffold.core.usecase.CombineUseCase

class CombineUseCaseStateBindingTest {

    @Test
    fun `use case can be constructed with a shared state holder`() = runTest {
        val sharedStateHolder = StateHolderImpl(CounterState(value = 10))
        val useCase = IncrementUseCase(stateHolder = sharedStateHolder)

        assertEquals(10, useCase.state.value.value)

        useCase.receiveEvent(CounterEvent.Increment)

        assertEquals(11, useCase.state.value.value)
        assertEquals(11, sharedStateHolder.state.value.value)
    }

    @Test
    fun `combine use case builds child use cases from the same shared state holder`() = runTest {
        val sharedStateHolder = StateHolderImpl(CounterState(value = 3))
        val combinedUseCase = CombineUseCase(
            sharedStateHolder,
            MutableServiceRegistryImpl(),
            { holder: StateHolder<CounterState>, registry -> IncrementUseCase(stateHolder = holder, serviceRegistry = registry) },
        )

        combinedUseCase.receiveEvent(CounterEvent.Increment)

        assertEquals(4, combinedUseCase.state.value.value)
        assertEquals(4, sharedStateHolder.state.value.value)
    }

    private data class CounterState(
        val value: Int = 0,
    ) : UiState

    private sealed interface CounterEvent : UiEvent {
        data object Increment : CounterEvent
    }

    private data object CounterEffect : UiEffect

    private class IncrementUseCase(
        stateHolder: StateHolder<CounterState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseUseCase<CounterState, CounterEvent, CounterEffect>(
        stateHolder = stateHolder ?: CounterState().toStateHolder(),
        serviceRegistry = serviceRegistry,
    ) {
        override suspend fun onEvent(event: CounterEvent) {
            when (event) {
                CounterEvent.Increment -> updateState { it.copy(value = it.value + 1) }
            }
        }
    }
}
