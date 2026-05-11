package zhaoyun.example.composedemo.scaffold.platform

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

@RunWith(AndroidJUnit4::class)
class ServiceRegistryComposeTest {
    @Test
    fun serviceRegistryProvider_exposes_registry_to_composition() {
        val registry = MutableServiceRegistryImpl()
        var seenRegistry: Any? = null

        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    ServiceRegistryProvider(registry = registry) {
                        val currentRegistry = LocalServiceRegistry.current
                        SideEffect {
                            seenRegistry = currentRegistry
                        }
                    }
                }
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            assertSame(registry, seenRegistry)
        }
    }

    @Test
    fun registerService_registers_while_composed_and_unregisters_when_removed() {
        val registry = MutableServiceRegistryImpl()
        val service = DemoService("demo")
        var show by mutableStateOf(true)
        val removed = CountDownLatch(1)

        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.setContent {
                    ServiceRegistryProvider(registry = registry) {
                        if (show) {
                            RegisterService<DemoContract>(service, tag = "story")
                        }
                        SideEffect {
                            if (!show) {
                                removed.countDown()
                            }
                        }
                    }
                }
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()

            assertSame(service, registry.find(DemoContract::class.java, tag = "story"))
            scenario.onActivity {
                show = false
            }

            assertTrue(removed.await(5, TimeUnit.SECONDS))
            assertNull(registry.find(DemoContract::class.java, tag = "story"))
        }
    }

    private interface DemoContract {
        fun name(): String
    }

    private data class DemoService(
        private val value: String,
    ) : DemoContract {
        override fun name(): String = value
    }
}
