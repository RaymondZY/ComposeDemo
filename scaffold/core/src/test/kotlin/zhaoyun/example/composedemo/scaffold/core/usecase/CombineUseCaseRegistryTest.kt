package zhaoyun.example.composedemo.scaffold.core.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertNotNull
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.MviService

class CombineUseCaseRegistryTest {

    interface DemoService : MviService {
        fun demo()
    }

    class DemoUseCase(stateHolder: StateHolder<TestState>, serviceRegistry: MutableServiceRegistry) : BaseUseCase<TestState, TestEvent, TestEffect>(stateHolder, serviceRegistry), DemoService {
        override fun demo() {}
        override suspend fun onEvent(event: TestEvent) {}
    }

    data object TestState : UiState
    data object TestEvent : UiEvent
    data object TestEffect : UiEffect

    @Test
    fun `child UseCase auto-registers in shared Screen registry`() {
        val registry = MutableServiceRegistryImpl()
        val stateHolder = TestStateHolder(TestState)
        val combineUseCase = CombineUseCase(stateHolder, registry, { h, r -> DemoUseCase(h, r) })

        assertNotNull(registry.find(DemoService::class.java))
        combineUseCase.onCleared()
    }

    class TestStateHolder(override val initialState: TestState) : StateHolder<TestState> {
        override val state = MutableStateFlow(initialState)
        override fun updateState(transform: (TestState) -> TestState) {
            state.value = transform(state.value)
        }

        override fun <D : UiState> derive(childSelector: (TestState) -> D, parentUpdater: TestState.(D) -> TestState): StateHolder<D> {
            throw NotImplementedError()
        }
    }
}
