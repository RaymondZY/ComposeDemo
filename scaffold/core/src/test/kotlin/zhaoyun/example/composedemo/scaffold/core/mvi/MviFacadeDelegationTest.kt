package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class MviFacadeDelegationTest {

    @Test
    fun `default facade implementations delegate to their owned components`() = runTest {
        val stateHolder = StateHolderImpl(DemoState(count = 1))
        val receivedEvents = mutableListOf<DemoEvent>()
        val eventReceiver = EventReceiverImpl<DemoEvent> { event ->
            receivedEvents += event
        }
        val effectDispatcher = EffectDispatcherImpl<DemoEffect>()

        val facade = object : MviFacade<DemoState, DemoEvent, DemoEffect> {
            override val stateHolder = stateHolder
            override val eventReceiver = eventReceiver
            override val effectDispatcher = effectDispatcher
        }

        val effectDeferred = async { facade.effect.first() }
        val baseEffectDeferred = async { facade.baseEffect.first() }

        assertEquals(DemoState(count = 1), facade.initialState)
        assertEquals(DemoState(count = 1), facade.currentState)

        facade.updateState { it.copy(count = it.count + 1) }
        facade.receiveEvent(DemoEvent.Increment)
        facade.dispatchEffect(DemoEffect("effect"))
        facade.dispatchBaseEffect(BaseEffect.ShowDialog("title", "message"))

        assertEquals(DemoState(count = 2), facade.currentState)
        assertEquals(listOf(DemoEvent.Increment), receivedEvents)
        assertEquals(DemoEffect("effect"), effectDeferred.await())
        assertEquals(BaseEffect.ShowDialog("title", "message"), baseEffectDeferred.await())
    }

    private data class DemoState(
        val count: Int = 0,
    ) : UiState

    private sealed interface DemoEvent : UiEvent {
        data object Increment : DemoEvent
    }

    private data class DemoEffect(
        val name: String,
    ) : UiEffect
}
