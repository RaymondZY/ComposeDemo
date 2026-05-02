package zhaoyun.example.composedemo.scaffold.core.usecase

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.*

class CombineUseCaseRegistryTest {

    interface DemoService : MviService {
        fun demo()
    }

    class DemoUseCase(stateHolder: StateHolder<TestState>) : BaseUseCase<TestState, TestEvent, TestEffect>(stateHolder), DemoService {
        override fun demo() {}
        override suspend fun onEvent(event: TestEvent) {}
    }

    data object TestState : UiState
    data object TestEvent : UiEvent
    data object TestEffect : UiEffect

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
    fun `child UseCase auto-registers in shared Screen registry`() {
        val koin = org.koin.core.context.GlobalContext.get()
        val scope = koin.createScope("test", named("MviScreenScope"))
        ScreenScopeStack.push(scope)

        val stateHolder = TestStateHolder(TestState)
        val combineUseCase = CombineUseCase(stateHolder, { DemoUseCase(it) })

        val registry = scope.get<MutableServiceRegistry>()
        assertNotNull(registry.find(DemoService::class.java))

        ScreenScopeStack.pop()
        scope.close()
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
