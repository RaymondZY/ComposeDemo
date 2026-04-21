package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BaseUseCaseReducerBindTest {

    data class TestState(val value: Int = 0) : UiState
    object TestEvent : UiEvent
    object TestEffect : UiEffect

    class TestUseCase : BaseUseCase<TestState, TestEvent, TestEffect>(TestState(0)) {
        fun increment() = updateState { it.copy(value = it.value + 1) }
        fun getCurrent(): TestState = currentState

        override suspend fun onEvent(event: TestEvent) {
            // no-op for test
        }
    }

    @Test
    fun `未bind时使用internalState`() {
        val useCase = TestUseCase()
        assertEquals(0, useCase.state.value.value)

        useCase.increment()
        assertEquals(1, useCase.state.value.value)
        assertEquals(1, useCase.getCurrent().value)
    }

    @Test
    fun `bind后updateState路由到reducer`() = runTest {
        val useCase = TestUseCase()
        val reducer = LocalReducer(TestState(100))

        useCase.bind(reducer)
        assertEquals(100, useCase.state.value.value)

        useCase.increment()
        assertEquals(101, useCase.state.value.value)
        assertEquals(101, reducer.state.value.value)
        assertEquals(101, useCase.getCurrent().value)
    }

    @Test
    fun `bind后多个useCase共享同一个reducer`() = runTest {
        val reducer = LocalReducer(TestState(0))
        val useCaseA = TestUseCase()
        val useCaseB = TestUseCase()

        useCaseA.bind(reducer)
        useCaseB.bind(reducer)

        useCaseA.increment()
        assertEquals(1, useCaseA.state.value.value)
        assertEquals(1, useCaseB.state.value.value)

        useCaseB.increment()
        assertEquals(2, useCaseA.state.value.value)
        assertEquals(2, useCaseB.state.value.value)
    }
}
