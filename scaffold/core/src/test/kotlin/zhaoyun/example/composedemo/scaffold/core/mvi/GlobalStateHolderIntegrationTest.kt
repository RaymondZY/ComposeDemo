package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Global与DetailStateHolder集成测试
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GlobalStateHolderIntegrationTest {

    data class DetailState(val count: Int, val label: String) : UiState
    data class GlobalState(
        val detail: DetailState,
        val totalUpdates: Int = 0
    )

    @Test
    fun `DetailStateHolder通过DelegateStateHolder正确写回GlobalState`() = runTest {
        val globalState = MutableStateFlow(
            GlobalState(detail = DetailState(0, "A"))
        )

        val detailStateHolder = DelegateStateHolder(
            state = MutableStateFlow(globalState.value.detail),
            onUpdate = { transform ->
                globalState.value = globalState.value.copy(
                    detail = transform(globalState.value.detail)
                )
            }
        )

        detailStateHolder.update { it.copy(count = it.count + 1) }
        assertEquals(1, globalState.value.detail.count)
        assertEquals("A", globalState.value.detail.label)

        detailStateHolder.update { it.copy(label = "B") }
        assertEquals(1, globalState.value.detail.count)
        assertEquals("B", globalState.value.detail.label)
    }

    @Test
    fun `StateHolder拦截器可在Detail更新时同步修改Global其他字段`() = runTest {
        val globalState = MutableStateFlow(
            GlobalState(detail = DetailState(0, "A"), totalUpdates = 0)
        )

        val detailStateHolder = DelegateStateHolder(
            state = MutableStateFlow(globalState.value.detail),
            onUpdate = { transform ->
                val newDetail = transform(globalState.value.detail)
                globalState.value = globalState.value.copy(
                    detail = newDetail,
                    totalUpdates = globalState.value.totalUpdates + 1
                )
            }
        )

        detailStateHolder.update { it.copy(count = 1) }
        assertEquals(1, globalState.value.totalUpdates)

        detailStateHolder.update { it.copy(count = 2) }
        assertEquals(2, globalState.value.totalUpdates)

        detailStateHolder.update { it.copy(label = "Z") }
        assertEquals(3, globalState.value.totalUpdates)
    }

    @Test
    fun `多个DetailStateHolder代理到同一个GlobalState的不同切片`() = runTest {
        data class SliceA(val value: Int) : UiState
        data class SliceB(val value: Int) : UiState
        data class MultiGlobalState(val a: SliceA, val b: SliceB)

        val globalState = MutableStateFlow(MultiGlobalState(SliceA(0), SliceB(0)))

        val stateHolderA = DelegateStateHolder(
            state = MutableStateFlow(globalState.value.a),
            onUpdate = { transform ->
                globalState.value = globalState.value.copy(
                    a = transform(globalState.value.a)
                )
            }
        )

        val stateHolderB = DelegateStateHolder(
            state = MutableStateFlow(globalState.value.b),
            onUpdate = { transform ->
                globalState.value = globalState.value.copy(
                    b = transform(globalState.value.b)
                )
            }
        )

        stateHolderA.update { it.copy(value = 10) }
        stateHolderB.update { it.copy(value = 20) }

        assertEquals(10, globalState.value.a.value)
        assertEquals(20, globalState.value.b.value)
    }
}
