package zhaoyun.example.composedemo.story.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

class StoryCardUseCaseTest {

    @Test
    fun `placeholder events keep state unchanged`() = runTest {
        val initialState = StoryCardState()
        val useCase = StoryCardUseCase(
            initialState.toStateHolder(),
            MutableServiceRegistryImpl(),
        )

        useCase.receiveEvent(StoryCardEvent.OnRefresh)
        useCase.receiveEvent(StoryCardEvent.OnLoadMore)
        useCase.receiveEvent(StoryCardEvent.Message.OnDialogueClicked)
        useCase.receiveEvent(StoryCardEvent.InfoBar.OnLikeClicked)
        useCase.receiveEvent(StoryCardEvent.Input.OnFocused)

        assertEquals(initialState, useCase.state.value)
    }
}
