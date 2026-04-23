package zhaoyun.example.composedemo.story.message.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageUseCaseTest {

    private val useCase = MessageUseCase()

    @Test
    fun `初始状态isExpanded为false`() {
        assertFalse(useCase.state.value.isExpanded)
    }

    @Test
    fun `点击对白切换isExpanded为true`() = runTest {
        useCase.onEvent(MessageEvent.OnDialogueClicked)
        assertTrue(useCase.state.value.isExpanded)
    }

    @Test
    fun `再次点击对白恢复isExpanded为false`() = runTest {
        useCase.onEvent(MessageEvent.OnDialogueClicked)
        useCase.onEvent(MessageEvent.OnDialogueClicked)
        assertFalse(useCase.state.value.isExpanded)
    }
}
