package zhaoyun.example.composedemo.story.message.domain

import org.junit.Assert.assertFalse
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
}
