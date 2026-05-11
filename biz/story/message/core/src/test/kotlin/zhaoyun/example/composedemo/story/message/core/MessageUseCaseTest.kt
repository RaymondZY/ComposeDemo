package zhaoyun.example.composedemo.story.message.core

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

class MessageUseCaseTest {

    private val useCase = MessageUseCase(
        MessageState().toStateHolder(),
        MutableServiceRegistryImpl(),
    )

    @Test
    fun `初始状态isExpanded为false`() {
        assertFalse(useCase.state.value.isExpanded)
    }

    @Test
    fun `点击对白切换isExpanded为true`() = runTest {
        useCase.receiveEvent(MessageEvent.OnDialogueClicked)
        assertTrue(useCase.state.value.isExpanded)
    }

    @Test
    fun `再次点击对白恢复isExpanded为false`() = runTest {
        useCase.receiveEvent(MessageEvent.OnDialogueClicked)
        useCase.receiveEvent(MessageEvent.OnDialogueClicked)
        assertFalse(useCase.state.value.isExpanded)
    }

    @Test
    fun `点击对白切换状态时保留角色和对白内容`() = runTest {
        val useCase = MessageUseCase(
            MessageState(
                characterName = "Hero",
                characterSubtitle = "Guide",
                dialogueText = "Hello",
            ).toStateHolder(),
            MutableServiceRegistryImpl(),
        )

        useCase.receiveEvent(MessageEvent.OnDialogueClicked)

        val state = useCase.state.value
        assertTrue(state.isExpanded)
        assertEquals("Hero", state.characterName)
        assertEquals("Guide", state.characterSubtitle)
        assertEquals("Hello", state.dialogueText)
    }

    @Test
    fun `多次连续点击对白按奇偶次数切换状态`() = runTest {
        repeat(5) {
            useCase.receiveEvent(MessageEvent.OnDialogueClicked)
        }
        assertTrue(useCase.state.value.isExpanded)

        useCase.receiveEvent(MessageEvent.OnDialogueClicked)
        assertFalse(useCase.state.value.isExpanded)
    }
}
