package zhaoyun.example.composedemo.scaffold.android

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolderImpl
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `view model shares one registry across its child use cases`() = runTest {
        val viewModel = DemoViewModel()

        viewModel.receiveEvent(DemoEvent.RegisterAndRead)

        assertEquals("registered", viewModel.state.value.resolvedName)
    }

    @Test
    fun `sendEvent processes events on the viewModel scope`() = runTest(mainDispatcherRule.dispatcher) {
        val viewModel = DemoViewModel()

        viewModel.sendEvent(DemoEvent.Increment)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.count)
    }

    @Test
    fun `view model register and unregister service APIs operate on the owned registry`() {
        val viewModel = DemoViewModel()
        val service = DemoService("vm")

        viewModel.registerService(DemoNamedService::class.java, service, tag = "vm")
        assertSame(service, viewModel.serviceRegistry.find(DemoNamedService::class.java, tag = "vm"))

        viewModel.unregisterService(DemoNamedService::class.java, tag = "vm")
        assertNull(viewModel.serviceRegistry.find(DemoNamedService::class.java, tag = "vm"))
    }

    @Test
    fun `view model registry falls back to parent scope and clears on onCleared`() {
        val parentRegistry = MutableServiceRegistryImpl()
        val parentService = DemoService("parent")
        parentRegistry.register(DemoNamedService::class.java, parentService)

        val viewModel = DemoViewModel(parentRegistry = parentRegistry)
        val localService = DemoService("local")
        viewModel.registerService(DemoNamedService::class.java, localService, tag = "local")

        assertSame(parentService, viewModel.serviceRegistry.find(DemoNamedService::class.java))
        assertSame(localService, viewModel.serviceRegistry.find(DemoNamedService::class.java, tag = "local"))

        viewModel.clearForTest()

        assertSame(parentService, viewModel.serviceRegistry.find(DemoNamedService::class.java))
        assertNull(viewModel.serviceRegistry.find(DemoNamedService::class.java, tag = "local"))
    }

    @Test
    fun `view model can be constructed with a shared state holder`() {
        val sharedStateHolder = StateHolderImpl(DemoState(count = 3))
        val viewModel = DemoViewModel(stateHolder = sharedStateHolder)

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

    private interface DemoNamedService {
        fun name(): String
    }

    private data class DemoService(
        private val value: String,
    ) : DemoNamedService {
        override fun name(): String = value
    }

    private class ProviderUseCase(
        stateHolder: StateHolder<DemoState>? = null,
    ) : zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        initialState = DemoState(),
        stateHolder = stateHolder,
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
    ) : zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        initialState = DemoState(),
        stateHolder = stateHolder,
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
        parentRegistry: zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry? = null,
    ) : BaseViewModel<DemoState, DemoEvent, DemoEffect>(
        initialState = DemoState(),
        { holder -> ProviderUseCase(stateHolder = holder) },
        { holder -> ConsumerUseCase(stateHolder = holder) },
        stateHolder = stateHolder,
        parentServiceRegistry = parentRegistry,
    ) {
        fun clearForTest() {
            onCleared()
        }
    }
}
