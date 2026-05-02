package zhaoyun.example.composedemo.scaffold.core.spi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class MutableServiceRegistryImplTest {

    interface DemoService {
        fun id(): String
    }

    private data class Demo(
        private val value: String,
    ) : DemoService {
        override fun id(): String = value
    }

    @Test(expected = IllegalStateException::class)
    fun `duplicate untagged service registration in same scope fails fast`() {
        val registry = MutableServiceRegistryImpl()

        registry.register(DemoService::class.java, Demo("first"))
        registry.register(DemoService::class.java, Demo("second"))
    }

    @Test
    fun `same type with different tags coexists in same scope`() {
        val registry = MutableServiceRegistryImpl()
        val first = Demo("first")
        val second = Demo("second")

        registry.register(DemoService::class.java, first, tag = "a")
        registry.register(DemoService::class.java, second, tag = "b")

        assertSame(first, registry.find(DemoService::class.java, tag = "a"))
        assertSame(second, registry.find(DemoService::class.java, tag = "b"))
    }

    @Test(expected = IllegalStateException::class)
    fun `duplicate tagged service registration in same scope fails fast`() {
        val registry = MutableServiceRegistryImpl()

        registry.register(DemoService::class.java, Demo("first"), tag = "story")
        registry.register(DemoService::class.java, Demo("second"), tag = "story")
    }

    @Test
    fun `child scope falls back to parent scope`() {
        val parent = MutableServiceRegistryImpl()
        val child = MutableServiceRegistryImpl(parent = parent)
        val service = Demo("parent")

        parent.register(DemoService::class.java, service)

        assertSame(service, child.find(DemoService::class.java))
        assertEquals("parent", child.find(DemoService::class.java)?.id())
    }

    @Test
    fun `child scope prefers local registration over parent registration`() {
        val parent = MutableServiceRegistryImpl()
        val child = MutableServiceRegistryImpl(parent = parent)
        val parentService = Demo("parent")
        val childService = Demo("child")

        parent.register(DemoService::class.java, parentService)
        child.register(DemoService::class.java, childService)

        assertSame(childService, child.find(DemoService::class.java))
        assertEquals("child", child.find(DemoService::class.java)?.id())
    }

    @Test
    fun `unregister by type removes only local registration and reveals parent fallback`() {
        val parent = MutableServiceRegistryImpl()
        val child = MutableServiceRegistryImpl(parent = parent)
        val parentService = Demo("parent")
        val childService = Demo("child")

        parent.register(DemoService::class.java, parentService, tag = "story")
        child.register(DemoService::class.java, childService, tag = "story")

        child.unregister(DemoService::class.java, tag = "story")

        assertSame(parentService, child.find(DemoService::class.java, tag = "story"))
    }

    @Test
    fun `unregister by instance removes every local alias for that object`() {
        val registry = MutableServiceRegistryImpl()
        val service = Demo("shared")

        registry.register(DemoService::class.java, service)
        registry.register(DemoService::class.java, service, tag = "story")

        registry.unregister(service)

        assertNull(registry.find(DemoService::class.java))
        assertNull(registry.find(DemoService::class.java, tag = "story"))
    }

    @Test
    fun `clear only clears the current scope and keeps parent fallback available`() {
        val parent = MutableServiceRegistryImpl()
        val child = MutableServiceRegistryImpl(parent = parent)
        val parentService = Demo("parent")
        val childService = Demo("child")

        parent.register(DemoService::class.java, parentService)
        child.register(DemoService::class.java, childService, tag = "story")

        child.clear()

        assertSame(parentService, child.find(DemoService::class.java))
        assertNull(child.find(DemoService::class.java, tag = "story"))
    }
}
