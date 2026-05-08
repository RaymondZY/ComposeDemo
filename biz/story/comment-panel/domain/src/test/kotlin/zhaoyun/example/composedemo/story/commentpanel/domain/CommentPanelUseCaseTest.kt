package zhaoyun.example.composedemo.story.commentpanel.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

class CommentPanelUseCaseTest {
    @Test
    fun initialState_keepsCardIdOnly() = runTest {
        val useCase = CommentPanelUseCase(
            stateHolder = CommentPanelState(cardId = "story-1").toStateHolder(),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        assertEquals(CommentPanelState(cardId = "story-1"), useCase.state.value)
    }
}
