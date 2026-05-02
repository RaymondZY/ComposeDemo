package zhaoyun.example.composedemo.scaffold.android

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

@RunWith(AndroidJUnit4::class)
class ServiceRegistryComposeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun serviceRegistryProvider_exposes_registry_to_composition() {
        val registry = MutableServiceRegistryImpl()
        var seenRegistry: Any? = null

        composeRule.setContent {
            ServiceRegistryProvider(registry = registry) {
                val currentRegistry = LocalServiceRegistry.current
                SideEffect {
                    seenRegistry = currentRegistry
                }
            }
        }

        composeRule.runOnIdle {
            assertSame(registry, seenRegistry)
        }
    }

    @Test
    fun registerService_registers_while_composed_and_unregisters_when_removed() {
        val registry = MutableServiceRegistryImpl()
        val service = DemoService("demo")
        var show by mutableStateOf(true)

        composeRule.setContent {
            ServiceRegistryProvider(registry = registry) {
                if (show) {
                    RegisterService<DemoContract>(service, tag = "story")
                }
            }
        }

        composeRule.runOnIdle {
            assertSame(service, registry.find(DemoContract::class.java, tag = "story"))
            show = false
        }

        composeRule.waitForIdle()

        composeRule.runOnIdle {
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
