package zhaoyun.example.composedemo.scaffold.core.usecase

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

class CombineUseCaseBehaviorTest {

    @Test
    fun `combine use case fans a single event out to all child use cases`() = runTest {
        val combineUseCase = CombineUseCase(
            DemoState().toStateHolder(),
            MutableServiceRegistryImpl(),
            { holder: StateHolder<DemoState>, registry -> LeftCounterUseCase(stateHolder = holder, serviceRegistry = registry) },
            { holder: StateHolder<DemoState>, registry -> RightCounterUseCase(stateHolder = holder, serviceRegistry = registry) },
        )

        combineUseCase.receiveEvent(DemoEvent.Count)

        assertEquals(DemoState(leftCount = 1, rightCount = 1), combineUseCase.state.value)
    }

    @Test
    fun `combine use case merges child effects and its own dispatched effects`() = runTest {
        val combineUseCase = CombineUseCase(
            DemoState().toStateHolder(),
            MutableServiceRegistryImpl(),
            { holder: StateHolder<DemoState>, registry -> EffectEmitterUseCase(stateHolder = holder, serviceRegistry = registry) },
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
            MutableServiceRegistryImpl(),
            { holder: StateHolder<DemoState>, registry -> EffectEmitterUseCase(stateHolder = holder, serviceRegistry = registry) },
        )

        val childBaseEffect = async { combineUseCase.baseEffect.first() }
        combineUseCase.receiveEvent(DemoEvent.EmitBaseEffect)
        assertEquals(BaseEffect.ShowToast("child"), childBaseEffect.await())

        val ownBaseEffect = async { combineUseCase.baseEffect.first() }
        combineUseCase.dispatchBaseEffect(BaseEffect.NavigateBack)
        assertEquals(BaseEffect.NavigateBack, ownBaseEffect.await())
    }

    @Test
    fun `combine use case forwards cleared callback to child use cases`() {
        lateinit var childUseCase: ClearTrackingUseCase
        val combineUseCase = CombineUseCase(
            DemoState().toStateHolder(),
            MutableServiceRegistryImpl(),
            { holder: StateHolder<DemoState>, registry ->
                ClearTrackingUseCase(stateHolder = holder, serviceRegistry = registry).also {
                    childUseCase = it
                }
            },
        )

        combineUseCase.onCleared()

        assertTrue(childUseCase.wasCleared)
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
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
        serviceRegistry = serviceRegistry,
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            if (event == DemoEvent.Count) {
                updateState { it.copy(leftCount = it.leftCount + 1) }
            }
        }
    }

    private class RightCounterUseCase(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
        serviceRegistry = serviceRegistry,
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            if (event == DemoEvent.Count) {
                updateState { it.copy(rightCount = it.rightCount + 1) }
            }
        }
    }

    private class EffectEmitterUseCase(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
        serviceRegistry = serviceRegistry,
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            when (event) {
                DemoEvent.EmitEffect -> dispatchEffect(DemoEffect("child"))
                DemoEvent.EmitBaseEffect -> dispatchBaseEffect(BaseEffect.ShowToast("child"))
                else -> Unit
            }
        }
    }

    private class ClearTrackingUseCase(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
        serviceRegistry = serviceRegistry,
    ) {
        var wasCleared: Boolean = false
            private set

        override suspend fun onEvent(event: DemoEvent) = Unit

        override fun onCleared() {
            wasCleared = true
        }
    }
}
