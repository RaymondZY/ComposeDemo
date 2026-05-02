package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StateHolderTest {

    @Test
    fun `state holder exposes initial and current state`() {
        val holder = StateHolderImpl(ParentState(title = "hello", child = ChildState(count = 2)))

        assertEquals(ParentState(title = "hello", child = ChildState(count = 2)), holder.initialState)
        assertEquals(ParentState(title = "hello", child = ChildState(count = 2)), holder.currentState)
    }

    @Test
    fun `derived holder exposes the child slice of initial state`() {
        val holder = StateHolderImpl(ParentState(title = "hello", child = ChildState(count = 2)))
        val childHolder = holder.derive(ParentState::child) { copy(child = it) }

        assertEquals(ChildState(count = 2), childHolder.initialState)
        assertEquals(ChildState(count = 2), childHolder.currentState)
    }

    @Test
    fun `updating derived holder writes through to parent state`() {
        val holder = StateHolderImpl(ParentState(title = "hello", child = ChildState(count = 2)))
        val childHolder = holder.derive(ParentState::child) { copy(child = it) }

        childHolder.updateState { it.copy(count = 3) }

        assertEquals(ChildState(count = 3), childHolder.currentState)
        assertEquals(ParentState(title = "hello", child = ChildState(count = 3)), holder.currentState)
    }

    @Test
    fun `parent updates propagate to the derived holder`() {
        val holder = StateHolderImpl(ParentState(title = "hello", child = ChildState(count = 2)))
        val childHolder = holder.derive(ParentState::child) { copy(child = it) }

        holder.updateState { it.copy(child = ChildState(count = 4)) }

        assertEquals(ChildState(count = 4), childHolder.currentState)
    }

    @Test
    fun `derived state flow only emits when the child slice changes`() = runTest {
        val holder = StateHolderImpl(ParentState(title = "hello", child = ChildState(count = 2)))
        val childHolder = holder.derive(ParentState::child) { copy(child = it) }
        val emissions = mutableListOf<ChildState>()

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            childHolder.state.take(2).toList(emissions)
        }

        advanceUntilIdle()
        assertEquals(listOf(ChildState(count = 2)), emissions)

        holder.updateState { it.copy(title = "world") }
        advanceUntilIdle()
        assertEquals(listOf(ChildState(count = 2)), emissions)

        holder.updateState { it.copy(child = ChildState(count = 5)) }
        advanceUntilIdle()
        assertEquals(listOf(ChildState(count = 2), ChildState(count = 5)), emissions)

        job.cancel()
    }

    private data class ParentState(
        val title: String = "",
        val child: ChildState = ChildState(),
    ) : UiState

    private data class ChildState(
        val count: Int = 0,
    ) : UiState
}
