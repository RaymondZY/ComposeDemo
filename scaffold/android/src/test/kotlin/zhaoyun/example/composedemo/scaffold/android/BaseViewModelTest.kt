package zhaoyun.example.composedemo.scaffold.android

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolderImpl
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.MviService
import zhaoyun.example.composedemo.scaffold.core.spi.ScreenScopeStack

@OptIn(ExperimentalCoroutinesApi::class)
class BaseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setup() {
        startKoin {
            modules(module {
                scope(named("MviScreenScope")) {
                    scoped<MutableServiceRegistry> { MutableServiceRegistryImpl() }
                }
            })
        }
    }

    @After
    fun teardown() {
        stopKoin()
        while (ScreenScopeStack.current != null) {
            ScreenScopeStack.pop()
        }
    }

    @Test
    fun `view model shares one registry across its child use cases`() = runTest {
        val koin = org.koin.core.context.GlobalContext.get()
        val scope = koin.createScope("test", named("MviScreenScope"))
        ScreenScopeStack.push(scope)

        val viewModel = DemoViewModel()

        viewModel.receiveEvent(DemoEvent.RegisterAndRead)

        assertEquals("registered", viewModel.state.value.resolvedName)

        ScreenScopeStack.pop()
        scope.close()
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
        val koin = org.koin.core.context.GlobalContext.get()
        val scope = koin.createScope("test2", named("MviScreenScope"))
        ScreenScopeStack.push(scope)

        val viewModel = DemoViewModel()
        val service = DemoService("vm")

        viewModel.registerService(DemoNamedService::class.java, service, tag = "vm")
        val registry = scope.get<MutableServiceRegistry>()
        assertSame(service, registry.find(DemoNamedService::class.java, tag = "vm"))

        viewModel.unregisterService(DemoNamedService::class.java, tag = "vm")
        assertNull(registry.find(DemoNamedService::class.java, tag = "vm"))

        ScreenScopeStack.pop()
        scope.close()
    }

    @Test
    fun `viewModel ensureRegistered is idempotent`() {
        val registry = MutableServiceRegistryImpl()
        val viewModel = DemoViewModel()

        viewModel.ensureRegistered(registry)
        viewModel.ensureRegistered(registry) // should not crash or duplicate

        val service = DemoService("local")
        viewModel.registerService(DemoNamedService::class.java, service, tag = "local")
        assertSame(service, registry.find(DemoNamedService::class.java, tag = "local"))
    }

    @Test
    fun `viewModel onCleared handles null registry gracefully`() {
        val viewModel = DemoViewModel()
        viewModel.clearForTest() // should not crash even though screenRegistry is null
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
    ) : zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: StateHolderImpl(DemoState()),
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
        stateHolder = stateHolder ?: StateHolderImpl(DemoState()),
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
    ) : BaseViewModel<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: StateHolderImpl(DemoState()),
        { holder -> ProviderUseCase(stateHolder = holder) },
        { holder -> ConsumerUseCase(stateHolder = holder) },
    ) {
        fun clearForTest() {
            onCleared()
        }
    }
}
