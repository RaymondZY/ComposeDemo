package zhaoyun.example.composedemo.scaffold.core.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.findService
import zhaoyun.example.composedemo.scaffold.core.spi.findServiceOrNull
import zhaoyun.example.composedemo.scaffold.core.spi.registerService
import zhaoyun.example.composedemo.scaffold.core.spi.unregisterService

class BaseUseCaseDynamicRegistrationTest {

    @Test
    fun `use case can dynamically register and unregister services in current scope`() = runTest {
        val registry = MutableServiceRegistryImpl()
        val combineUseCase = CombineUseCase(
            DemoState().toStateHolder(),
            registry,
            { holder: StateHolder<DemoState>, reg -> DynamicProviderUseCase(stateHolder = holder, serviceRegistry = reg) },
            { holder: StateHolder<DemoState>, reg -> DynamicConsumerUseCase(stateHolder = holder, serviceRegistry = reg) },
        )

        combineUseCase.receiveEvent(DemoEvent.Register)
        combineUseCase.receiveEvent(DemoEvent.Read)
        assertEquals("dynamic", combineUseCase.state.value.resolvedId)
        assertTrue(combineUseCase.state.value.hasService)

        combineUseCase.receiveEvent(DemoEvent.Unregister)
        combineUseCase.receiveEvent(DemoEvent.CheckMissing)
        assertFalse(combineUseCase.state.value.hasService)

        combineUseCase.onCleared()
    }

    @Test
    fun `find service or null returns null before a dynamic registration happens`() = runTest {
        val registry = MutableServiceRegistryImpl()
        val combineUseCase = CombineUseCase(
            DemoState().toStateHolder(),
            registry,
            { holder: StateHolder<DemoState>, reg -> DynamicConsumerUseCase(stateHolder = holder, serviceRegistry = reg) },
        )

        combineUseCase.receiveEvent(DemoEvent.CheckMissing)

        assertFalse(combineUseCase.state.value.hasService)
        assertNull(combineUseCase.state.value.resolvedId)

        combineUseCase.onCleared()
    }

    @Test(expected = IllegalStateException::class)
    fun `register service fails fast when duplicate registration happens`() = runTest {
        val registry = MutableServiceRegistryImpl()
        val useCase = DynamicProviderUseCase(serviceRegistry = registry)
        useCase.receiveEvent(DemoEvent.Register)
        useCase.receiveEvent(DemoEvent.Register) // duplicate should fail
    }

    private data class DemoState(
        val resolvedId: String? = null,
        val hasService: Boolean = false,
    ) : UiState

    private sealed interface DemoEvent : UiEvent {
        data object Register : DemoEvent
        data object Read : DemoEvent
        data object Unregister : DemoEvent
        data object CheckMissing : DemoEvent
    }

    private data object DemoEffect : UiEffect

    private interface DynamicService {
        fun id(): String
    }

    private data class DynamicServiceImpl(
        private val value: String,
    ) : DynamicService {
        override fun id(): String = value
    }

    private class DynamicProviderUseCase(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
        serviceRegistry = serviceRegistry,
    ) {
        private val service = DynamicServiceImpl("dynamic")

        override suspend fun onEvent(event: DemoEvent) {
            when (event) {
                DemoEvent.Register -> registerService<DynamicService>(service, tag = "story")
                DemoEvent.Unregister -> unregisterService<DynamicService>(tag = "story")
                else -> Unit
            }
        }
    }

    private class DynamicConsumerUseCase(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
        serviceRegistry = serviceRegistry,
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            when (event) {
                DemoEvent.Read -> {
                    val service = findService<DynamicService>(tag = "story")
                    updateState {
                        it.copy(
                            resolvedId = service.id(),
                            hasService = true,
                        )
                    }
                }

                DemoEvent.CheckMissing -> {
                    updateState {
                        it.copy(hasService = findServiceOrNull<DynamicService>(tag = "story") != null)
                    }
                }

                else -> Unit
            }
        }
    }
}
