package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReducerTest {

    data class TestState(val count: Int = 0) : UiState

    @Test
    fun `LocalReducer执行reduce后状态正确更新`() = runTest {
        val reducer = LocalReducer(TestState(0))
        assertEquals(0, reducer.state.value.count)

        reducer.reduce { it.copy(count = it.count + 1) }
        assertEquals(1, reducer.state.value.count)

        reducer.reduce { it.copy(count = it.count + 5) }
        assertEquals(6, reducer.state.value.count)
    }

    @Test
    fun `LocalReducer并发reduce保证原子性`() = runTest {
        val reducer = LocalReducer(TestState(0))
        repeat(100) {
            reducer.reduce { it.copy(count = it.count + 1) }
        }
        assertEquals(100, reducer.state.value.count)
    }

    @Test
    fun `DelegateReducer的onReduce被正确触发`() = runTest {
        val external = MutableStateFlow(TestState(10))
        var receivedTransform: ((TestState) -> TestState)? = null

        val reducer = DelegateReducer(
            state = external,
            onReduce = { transform ->
                receivedTransform = transform
                external.value = transform(external.value)
            }
        )

        reducer.reduce { it.copy(count = it.count + 3) }

        assertEquals(13, external.value.count)
        assertEquals(13, reducer.state.first().count)
        assertEquals(13, receivedTransform?.invoke(TestState(10))?.count)
    }

    @Test
    fun `DelegateReducer的stateFlow与外部源同步`() = runTest {
        val external = MutableStateFlow(TestState(5))
        val reducer = DelegateReducer(
            state = external,
            onReduce = { transform -> external.value = transform(external.value) }
        )

        assertEquals(5, reducer.state.value.count)

        external.value = TestState(20)
        assertEquals(20, reducer.state.value.count)
    }
}
