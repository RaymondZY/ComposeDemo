package zhaoyun.example.composedemo.story.message.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.MutableServiceRegistry

class MessageUseCaseTest {

    private val useCase = MessageUseCase().apply {
        val registry = TestMutableServiceRegistry().apply {
            register(MessageAnalytics::class.java, object : MessageAnalytics {
                override fun trackMessageClicked() {}
                override fun trackMessageExpanded(expanded: Boolean) {}
            })
        }
        attachServiceRegistry(registry)
    }

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

    private class TestMutableServiceRegistry : MutableServiceRegistry {
        private val services = mutableMapOf<Class<*>, Any>()
        override fun <T : Any> register(clazz: Class<T>, instance: T) { services[clazz] = instance }
        override fun unregister(instance: Any) { services.values.remove(instance) }
        override fun clear() { services.clear() }
        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> find(clazz: Class<T>): T? = services[clazz] as? T
    }
}
