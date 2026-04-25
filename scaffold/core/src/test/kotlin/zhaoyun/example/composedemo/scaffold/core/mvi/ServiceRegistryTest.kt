package zhaoyun.example.composedemo.scaffold.core.mvi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ServiceRegistryTest {

    interface TestService {
        fun doSomething(): String
    }

    class TestServiceImpl : TestService {
        override fun doSomething() = "hello"
    }

    class TestServiceProvider : ServiceProvider {
        val impl = TestServiceImpl()
        override fun provideServices(registry: MutableServiceRegistry) {
            registry.register(TestService::class.java, impl)
        }
    }

    @Test
    fun `本地注册的服务可以被找到`() {
        val registry = TestMutableServiceRegistry()
        val provider = TestServiceProvider()
        provider.provideServices(registry)

        val found = registry.find(TestService::class.java)
        assertSame(provider.impl, found)
        assertEquals("hello", found?.doSomething())
    }

    @Test
    fun `未注册的服务返回null`() {
        val registry = TestMutableServiceRegistry()
        assertNull(registry.find(TestService::class.java))
    }

    @Test
    fun `Parent registry 作为 fallback`() {
        val parent = TestMutableServiceRegistry()
        val parentImpl = TestServiceImpl()
        parent.register(TestService::class.java, parentImpl)

        val child = TestMutableServiceRegistry(parent = parent)
        val found = child.find(TestService::class.java)

        assertSame(parentImpl, found)
    }

    @Test
    fun `本地覆盖 Parent 的服务`() {
        val parent = TestMutableServiceRegistry()
        val parentImpl = TestServiceImpl()
        parent.register(TestService::class.java, parentImpl)

        val child = TestMutableServiceRegistry(parent = parent)
        val childImpl = TestServiceImpl()
        child.register(TestService::class.java, childImpl)

        val found = child.find(TestService::class.java)
        assertSame(childImpl, found)
    }

    @Test
    fun `UseCase findService 能找到已注册的服务`() {
        val registry = TestMutableServiceRegistry()
        val provider = TestServiceProvider()
        provider.provideServices(registry)

        val useCase = object : BaseUseCase<TestState, TestEvent, TestEffect>(TestState()) {
            override suspend fun onEvent(event: TestEvent) {}
            fun testFind(): TestService = findService()
        }
        useCase.attachServiceRegistry(registry)

        assertSame(provider.impl, useCase.testFind())
    }

    @Test(expected = IllegalStateException::class)
    fun `UseCase findService 找不到时抛异常`() {
        val registry = TestMutableServiceRegistry()
        val useCase = object : BaseUseCase<TestState, TestEvent, TestEffect>(TestState()) {
            override suspend fun onEvent(event: TestEvent) {}
            fun testFind(): TestService = findService()
        }
        useCase.attachServiceRegistry(registry)
        useCase.testFind()
    }

    @Test
    fun `UseCase findServiceOrNull 找不到时返回null`() {
        val registry = TestMutableServiceRegistry()
        val useCase = object : BaseUseCase<TestState, TestEvent, TestEffect>(TestState()) {
            override suspend fun onEvent(event: TestEvent) {}
            fun testFind(): TestService? = findServiceOrNull()
        }
        useCase.attachServiceRegistry(registry)

        assertNull(useCase.testFind())
    }

    @Test(expected = IllegalStateException::class)
    fun `UseCase findService 未attachRegistry时抛异常`() {
        val useCase = object : BaseUseCase<TestState, TestEvent, TestEffect>(TestState()) {
            override suspend fun onEvent(event: TestEvent) {}
            fun testFind(): TestService = findService()
        }
        useCase.testFind()
    }

    @Test
    fun `UseCase findServiceOrNull 未attachRegistry时返回null`() {
        val useCase = object : BaseUseCase<TestState, TestEvent, TestEffect>(TestState()) {
            override suspend fun onEvent(event: TestEvent) {}
            fun testFind(): TestService? = findServiceOrNull()
        }
        assertNull(useCase.testFind())
    }

    @Test
    fun `UseCase detachServiceRegistry 后findService返回null`() {
        val registry = TestMutableServiceRegistry()
        val provider = TestServiceProvider()
        provider.provideServices(registry)

        val useCase = object : BaseUseCase<TestState, TestEvent, TestEffect>(TestState()) {
            override suspend fun onEvent(event: TestEvent) {}
            fun testFind(): TestService? = findServiceOrNull()
        }
        useCase.attachServiceRegistry(registry)
        assertSame(provider.impl, useCase.testFind())

        useCase.detachServiceRegistry()
        assertNull(useCase.testFind())
    }

    private class TestMutableServiceRegistry(
        private val parent: ServiceRegistry? = null
    ) : MutableServiceRegistry {
        private val services = mutableMapOf<Class<*>, Any>()

        override fun <T : Any> register(clazz: Class<T>, instance: T) { services[clazz] = instance }
        override fun unregister(instance: Any) { services.values.remove(instance) }
        override fun clear() { services.clear() }
        @Suppress("UNCHECKED_CAST")
        override fun <T : Any> find(clazz: Class<T>): T? = services[clazz] as? T ?: parent?.find(clazz)
    }

    private data class TestState(val value: Int = 0) : UiState
    private object TestEvent : UiEvent
    private object TestEffect : UiEffect
}
