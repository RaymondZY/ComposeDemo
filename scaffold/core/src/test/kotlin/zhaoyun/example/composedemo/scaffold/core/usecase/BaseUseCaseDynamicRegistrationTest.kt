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

class BaseUseCaseDynamicRegistrationTest {

    @Test
    fun `use case can dynamically register and unregister services in current scope`() = runTest {
        val combineUseCase = CombineUseCase(
            initialState = DemoState(),
            { holder: StateHolder<DemoState> -> DynamicProviderUseCase(stateHolder = holder) },
            { holder: StateHolder<DemoState> -> DynamicConsumerUseCase(stateHolder = holder) },
        )

        combineUseCase.receiveEvent(DemoEvent.Register)
        combineUseCase.receiveEvent(DemoEvent.Read)
        assertEquals("dynamic", combineUseCase.state.value.resolvedId)
        assertTrue(combineUseCase.state.value.hasService)

        combineUseCase.receiveEvent(DemoEvent.Unregister)
        combineUseCase.receiveEvent(DemoEvent.CheckMissing)
        assertFalse(combineUseCase.state.value.hasService)
    }

    @Test
    fun `find service or null returns null before a dynamic registration happens`() = runTest {
        val combineUseCase = CombineUseCase(
            initialState = DemoState(),
            { holder: StateHolder<DemoState> -> DynamicConsumerUseCase(stateHolder = holder) },
        )

        combineUseCase.receiveEvent(DemoEvent.CheckMissing)

        assertFalse(combineUseCase.state.value.hasService)
        assertNull(combineUseCase.state.value.resolvedId)
    }

    @Test(expected = IllegalStateException::class)
    fun `register service fails fast when no mutable registry is attached`() = runTest {
        DynamicProviderUseCase().receiveEvent(DemoEvent.Register)
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
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        initialState = DemoState(),
        stateHolder = stateHolder,
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
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        initialState = DemoState(),
        stateHolder = stateHolder,
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
