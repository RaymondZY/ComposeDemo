package zhaoyun.example.composedemo.story.message.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

class MessageUseCaseTest {

    private val useCase = MessageUseCase(MessageState().toStateHolder()).apply {
        val registry = MutableServiceRegistryImpl().apply {
            register(MessageAnalytics::class.java, object : MessageAnalytics {
                override fun trackMessageClicked() {}
                override fun trackMessageExpanded(expanded: Boolean) {}
            })
        }
        this.attachParent(registry)
    }

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
}
