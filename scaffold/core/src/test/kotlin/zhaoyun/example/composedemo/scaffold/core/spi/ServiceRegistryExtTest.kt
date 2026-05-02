package zhaoyun.example.composedemo.scaffold.core.spi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ServiceRegistryExtTest {

    interface PlainService {
        fun doSomething(): String
    }

    interface AnalyticsService : MviService {
        fun track()
    }

    interface DetailedAnalyticsService : AnalyticsService

    interface TaggedAnalyticsService : MviService

    private class PlainServiceImpl : PlainService {
        override fun doSomething(): String = "plain"
    }

    private class AnalyticsServiceImpl : AnalyticsService {
        var trackCount = 0
        override fun track() {
            trackCount++
        }
    }

    private class DetailedAnalyticsServiceImpl : DetailedAnalyticsService {
        var trackCount = 0
        override fun track() {
            trackCount++
        }
    }

    private class TaggedAnalyticsServiceImpl : TaggedAnalyticsService, TaggedMviService {
        override val serviceTag: String = "story"
        var trackCount = 0
        fun track() {
            trackCount++
        }
    }

    @Test
    fun `autoRegister registers MviService implementations`() {
        val registry = MutableServiceRegistryImpl()
        val impl = AnalyticsServiceImpl()

        impl.autoRegister(registry)

        assertSame(impl, registry.find(AnalyticsService::class.java))
    }

    @Test
    fun `autoRegister registers hierarchical MviService implementations`() {
        val registry = MutableServiceRegistryImpl()
        val impl = DetailedAnalyticsServiceImpl()

        impl.autoRegister(registry)

        assertSame(impl, registry.find(DetailedAnalyticsService::class.java))
        assertSame(impl, registry.find(AnalyticsService::class.java))
    }

    @Test
    fun `autoRegister does NOT register plain interfaces`() {
        val registry = MutableServiceRegistryImpl()
        val impl = PlainServiceImpl()

        impl.autoRegister(registry)

        assertNull(registry.find(PlainService::class.java))
    }

    @Test
    fun `autoRegister registers TaggedMviService with tag`() {
        val registry = MutableServiceRegistryImpl()
        val impl = TaggedAnalyticsServiceImpl()

        impl.autoRegister(registry)

        assertSame(impl, registry.find(TaggedAnalyticsService::class.java, tag = "story"))
        assertNull(registry.find(TaggedAnalyticsService::class.java))
    }

    @Test
    fun `autoUnregister removes the instance`() {
        val registry = MutableServiceRegistryImpl()
        val impl = AnalyticsServiceImpl()

        impl.autoRegister(registry)
        assertNotNull(registry.find(AnalyticsService::class.java))

        impl.autoUnregister(registry)
        assertNull(registry.find(AnalyticsService::class.java))
    }

    @Test
    fun `autoUnregister removes all aliases for the instance`() {
        val registry = MutableServiceRegistryImpl()
        val impl = DetailedAnalyticsServiceImpl()

        impl.autoRegister(registry)
        assertNotNull(registry.find(DetailedAnalyticsService::class.java))
        assertNotNull(registry.find(AnalyticsService::class.java))

        impl.autoUnregister(registry)
        assertNull(registry.find(DetailedAnalyticsService::class.java))
        assertNull(registry.find(AnalyticsService::class.java))
    }
}
