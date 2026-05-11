package zhaoyun.example.composedemo.scaffold.platform

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolderImpl
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.MviService
import zhaoyun.example.composedemo.scaffold.core.spi.findService
import zhaoyun.example.composedemo.scaffold.core.spi.registerService

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `view model shares one registry across its child use cases`() = runTest {
        val registry = MutableServiceRegistryImpl()
        val viewModel = DemoViewModel(serviceRegistry = registry)

        viewModel.receiveEvent(DemoEvent.RegisterAndRead)

        assertEquals("registered", viewModel.state.value.resolvedName)
    }

    @Test
    fun `sendEvent processes events on the viewModel scope`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = DemoViewModel(serviceRegistry = MutableServiceRegistryImpl())

        viewModel.sendEvent(DemoEvent.Increment)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.count)
    }

    @Test
    fun `view model can be constructed with a shared state holder`() {
        val sharedStateHolder = StateHolderImpl(DemoState(count = 3))
        val viewModel = DemoViewModel(stateHolder = sharedStateHolder, serviceRegistry = MutableServiceRegistryImpl())

        assertEquals(3, viewModel.state.value.count)

        viewModel.updateState { it.copy(count = 4) }

        assertEquals(4, sharedStateHolder.state.value.count)
    }

    private data class DemoState(
        val count: Int = 0,
        val resolvedName: String? = null,
    ) : UiState

    private sealed interface DemoEvent : UiEvent {
        data object Increment : DemoEvent
        data object RegisterAndRead : DemoEvent
    }

    private data object DemoEffect : UiEffect

    private interface DemoNamedService : MviService {
        fun name(): String
    }

    private data class DemoService(
        private val value: String,
    ) : DemoNamedService {
        override fun name(): String = value
    }

    private class ProviderUseCase(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: StateHolderImpl(DemoState()),
        serviceRegistry = serviceRegistry,
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            when (event) {
                DemoEvent.RegisterAndRead -> registerService<DemoNamedService>(DemoService("registered"), tag = "demo")
                else -> Unit
            }
        }
    }

    private class ConsumerUseCase(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: StateHolderImpl(DemoState()),
        serviceRegistry = serviceRegistry,
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            when (event) {
                DemoEvent.Increment -> updateState { it.copy(count = it.count + 1) }
                DemoEvent.RegisterAndRead -> {
                    val service = findService<DemoNamedService>(tag = "demo")
                    updateState { it.copy(resolvedName = service.name()) }
                }
            }
        }
    }

    private class DemoViewModel(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseViewModel<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: StateHolderImpl(DemoState()),
        serviceRegistry = serviceRegistry,
        { holder, reg -> ProviderUseCase(stateHolder = holder, serviceRegistry = reg) },
        { holder, reg -> ConsumerUseCase(stateHolder = holder, serviceRegistry = reg) },
    )
}
