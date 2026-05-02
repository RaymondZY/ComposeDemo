package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EventReceiverImplTest {

    @Test
    fun `delegates received events to the supplied handler`() = runTest {
        val received = mutableListOf<DemoEvent>()
        val receiver = EventReceiverImpl<DemoEvent> { event ->
            received += event
        }

        receiver.receiveEvent(DemoEvent.Load)
        receiver.receiveEvent(DemoEvent.Refresh)

        assertEquals(listOf(DemoEvent.Load, DemoEvent.Refresh), received)
    }

    private sealed interface DemoEvent : UiEvent {
        data object Load : DemoEvent
        data object Refresh : DemoEvent
    }
}
