package zhaoyun.example.composedemo.scaffold.core.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder

class CombineUseCaseBehaviorTest {

    @Test
    fun `combine use case fans a single event out to all child use cases`() = runTest {
        val combineUseCase = CombineUseCase(
            DemoState().toStateHolder(),
            { holder: StateHolder<DemoState> -> LeftCounterUseCase(stateHolder = holder) },
            { holder: StateHolder<DemoState> -> RightCounterUseCase(stateHolder = holder) },
        )

        combineUseCase.receiveEvent(DemoEvent.Count)

        assertEquals(DemoState(leftCount = 1, rightCount = 1), combineUseCase.state.value)
    }

    @Test
    fun `combine use case merges child effects and its own dispatched effects`() = runTest {
        val combineUseCase = CombineUseCase(
            DemoState().toStateHolder(),
            { holder: StateHolder<DemoState> -> EffectEmitterUseCase(stateHolder = holder) },
        )

        val childEffect = async { combineUseCase.effect.first() }
        combineUseCase.receiveEvent(DemoEvent.EmitEffect)
        assertEquals(DemoEffect("child"), childEffect.await())

        val ownEffect = async { combineUseCase.effect.first() }
        combineUseCase.dispatchEffect(DemoEffect("own"))
        assertEquals(DemoEffect("own"), ownEffect.await())
    }

    @Test
    fun `combine use case merges child base effects and its own base effects`() = runTest {
        val combineUseCase = CombineUseCase(
            DemoState().toStateHolder(),
            { holder: StateHolder<DemoState> -> EffectEmitterUseCase(stateHolder = holder) },
        )

        val childBaseEffect = async { combineUseCase.baseEffect.first() }
        combineUseCase.receiveEvent(DemoEvent.EmitBaseEffect)
        assertEquals(BaseEffect.ShowToast("child"), childBaseEffect.await())

        val ownBaseEffect = async { combineUseCase.baseEffect.first() }
        combineUseCase.dispatchBaseEffect(BaseEffect.NavigateBack)
        assertEquals(BaseEffect.NavigateBack, ownBaseEffect.await())
    }

    private data class DemoState(
        val leftCount: Int = 0,
        val rightCount: Int = 0,
    ) : UiState

    private sealed interface DemoEvent : UiEvent {
        data object Count : DemoEvent
        data object EmitEffect : DemoEvent
        data object EmitBaseEffect : DemoEvent
    }

    private data class DemoEffect(
        val name: String,
    ) : UiEffect

    private class LeftCounterUseCase(
        stateHolder: StateHolder<DemoState>? = null,
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            if (event == DemoEvent.Count) {
                updateState { it.copy(leftCount = it.leftCount + 1) }
            }
        }
    }

    private class RightCounterUseCase(
        stateHolder: StateHolder<DemoState>? = null,
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            if (event == DemoEvent.Count) {
                updateState { it.copy(rightCount = it.rightCount + 1) }
            }
        }
    }

    private class EffectEmitterUseCase(
        stateHolder: StateHolder<DemoState>? = null,
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            when (event) {
                DemoEvent.EmitEffect -> dispatchEffect(DemoEffect("child"))
                DemoEvent.EmitBaseEffect -> dispatchBaseEffect(BaseEffect.ShowToast("child"))
                else -> Unit
            }
        }
    }
}
