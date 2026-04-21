package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Global与DetailReducer集成测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GlobalReducerIntegrationTest {

    data class DetailState(val count: Int, val label: String) : UiState
    data class GlobalState(
        val detail: DetailState,
        val totalUpdates: Int = 0
    )

    @Test
    fun `DetailReduce通过DelegateReducer正确写回GlobalState`() = runTest {
        val globalState = MutableStateFlow(
            GlobalState(detail = DetailState(0, "A"))
        )

        val detailReducer = DelegateReducer(
            state = MutableStateFlow(globalState.value.detail),
            onReduce = { transform ->
                globalState.value = globalState.value.copy(
                    detail = transform(globalState.value.detail)
                )
            }
        )

        detailReducer.reduce { it.copy(count = it.count + 1) }
        assertEquals(1, globalState.value.detail.count)
        assertEquals("A", globalState.value.detail.label)

        detailReducer.reduce { it.copy(label = "B") }
        assertEquals(1, globalState.value.detail.count)
        assertEquals("B", globalState.value.detail.label)
    }

    @Test
    fun `Reduce拦截器可在Detail更新时同步修改Global其他字段`() = runTest {
        val globalState = MutableStateFlow(
            GlobalState(detail = DetailState(0, "A"), totalUpdates = 0)
        )

        val detailReducer = DelegateReducer(
            state = MutableStateFlow(globalState.value.detail),
            onReduce = { transform ->
                val newDetail = transform(globalState.value.detail)
                globalState.value = globalState.value.copy(
                    detail = newDetail,
                    totalUpdates = globalState.value.totalUpdates + 1
                )
            }
        )

        detailReducer.reduce { it.copy(count = 1) }
        assertEquals(1, globalState.value.totalUpdates)

        detailReducer.reduce { it.copy(count = 2) }
        assertEquals(2, globalState.value.totalUpdates)

        detailReducer.reduce { it.copy(label = "Z") }
        assertEquals(3, globalState.value.totalUpdates)
    }

    @Test
    fun `多个DetailReducer代理到同一个GlobalState的不同切片`() = runTest {
        data class SliceA(val value: Int) : UiState
        data class SliceB(val value: Int) : UiState
        data class MultiGlobalState(val a: SliceA, val b: SliceB)

        val globalState = MutableStateFlow(MultiGlobalState(SliceA(0), SliceB(0)))

        val reducerA = DelegateReducer(
            state = MutableStateFlow(globalState.value.a),
            onReduce = { transform ->
                globalState.value = globalState.value.copy(
                    a = transform(globalState.value.a)
                )
            }
        )

        val reducerB = DelegateReducer(
            state = MutableStateFlow(globalState.value.b),
            onReduce = { transform ->
                globalState.value = globalState.value.copy(
                    b = transform(globalState.value.b)
                )
            }
        )

        reducerA.reduce { it.copy(value = 10) }
        reducerB.reduce { it.copy(value = 20) }

        assertEquals(10, globalState.value.a.value)
        assertEquals(20, globalState.value.b.value)
    }
}
