package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StateHolderTest {

    data class TestState(val count: Int = 0) : UiState

    @Test
    fun `LocalStateHolder执行update后状态正确更新`() = runTest {
        val stateHolder = LocalStateHolder(TestState(0))
        assertEquals(0, stateHolder.state.value.count)

        stateHolder.update { it.copy(count = it.count + 1) }
        assertEquals(1, stateHolder.state.value.count)

        stateHolder.update { it.copy(count = it.count + 5) }
        assertEquals(6, stateHolder.state.value.count)
    }

    @Test
    fun `LocalStateHolder并发update保证原子性`() = runTest {
        val stateHolder = LocalStateHolder(TestState(0))
        repeat(100) {
            stateHolder.update { it.copy(count = it.count + 1) }
        }
        assertEquals(100, stateHolder.state.value.count)
    }

    @Test
    fun `DelegateStateHolder的onUpdate被正确触发`() = runTest {
        val external = MutableStateFlow(TestState(10))
        var receivedTransform: ((TestState) -> TestState)? = null

        val stateHolder = DelegateStateHolder(
            state = external,
            onUpdate = { transform ->
                receivedTransform = transform
                external.value = transform(external.value)
            }
        )

        stateHolder.update { it.copy(count = it.count + 3) }

        assertEquals(13, external.value.count)
        assertEquals(13, stateHolder.state.first().count)
        assertEquals(13, receivedTransform?.invoke(TestState(10))?.count)
    }

    @Test
    fun `DelegateStateHolder的stateFlow与外部源同步`() = runTest {
        val external = MutableStateFlow(TestState(5))
        val stateHolder = DelegateStateHolder(
            state = external,
            onUpdate = { transform -> external.value = transform(external.value) }
        )

        assertEquals(5, stateHolder.state.value.count)

        external.value = TestState(20)
        assertEquals(20, stateHolder.state.value.count)
    }
}
