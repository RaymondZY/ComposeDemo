package zhaoyun.example.composedemo.scaffold.android

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.parameter.parametersOf
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry

@RunWith(AndroidJUnit4::class)
class ScreenViewModelTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    companion object {
        private val testModule = module {
            viewModel { (keyData: String, parentRegistry: ServiceRegistry) ->
                DefaultScreenViewModel(keyData = keyData, parentRegistrySeen = parentRegistry)
            }
            viewModel { (payload: ScreenPayload, parentRegistry: ServiceRegistry, marker: String) ->
                CustomScreenViewModel(
                    payload = payload,
                    parentRegistrySeen = parentRegistry,
                    marker = marker,
                )
            }
        }

        @BeforeClass
        @JvmStatic
        fun startKoinForTests() {
            stopKoin()
            startKoin {
                modules(testModule)
            }
        }

        @AfterClass
        @JvmStatic
        fun stopKoinForTests() {
            stopKoin()
        }
    }

    @Test
    fun screenViewModel_default_parameters_include_key_data_and_parent_registry() {
        val registry = MutableServiceRegistryImpl()
        var viewModel: DefaultScreenViewModel? = null

        composeRule.setContent {
            ServiceRegistryProvider(registry = registry) {
                val resolved: DefaultScreenViewModel = screenViewModel("story")
                SideEffect {
                    viewModel = resolved
                }
            }
        }

        composeRule.runOnIdle {
            assertNotNull(viewModel)
            assertEquals("story", viewModel?.keyData)
            assertSame(registry, viewModel?.parentRegistrySeen)
        }
    }

    @Test
    fun screenViewModel_custom_parameters_receive_parent_registry() {
        val registry = MutableServiceRegistryImpl()
        val payload = ScreenPayload("story")
        var viewModel: CustomScreenViewModel? = null

        composeRule.setContent {
            ServiceRegistryProvider(registry = registry) {
                val resolved: CustomScreenViewModel = screenViewModel(payload) { parentRegistry ->
                    parametersOf(payload, parentRegistry, "custom")
                }
                SideEffect {
                    viewModel = resolved
                }
            }
        }

        composeRule.runOnIdle {
            assertNotNull(viewModel)
            assertEquals(payload, viewModel?.payload)
            assertSame(registry, viewModel?.parentRegistrySeen)
            assertEquals("custom", viewModel?.marker)
        }
    }

    @Test
    fun screenViewModel_requires_localServiceRegistry() {
        assertThrows(IllegalStateException::class.java) {
            composeRule.setContent {
                screenViewModel<String, DefaultScreenViewModel>("missing")
            }
        }
    }

    private data object ScreenState : UiState

    private data object ScreenEvent : UiEvent

    private data object ScreenEffect : UiEffect

    private data class ScreenPayload(
        val id: String,
    )

    private class DefaultScreenViewModel(
        val keyData: String,
        val parentRegistrySeen: ServiceRegistry,
    ) : BaseViewModel<ScreenState, ScreenEvent, ScreenEffect>(
        initialState = ScreenState,
        parentServiceRegistry = parentRegistrySeen,
    )

    private class CustomScreenViewModel(
        val payload: ScreenPayload,
        val parentRegistrySeen: ServiceRegistry,
        val marker: String,
    ) : BaseViewModel<ScreenState, ScreenEvent, ScreenEffect>(
        initialState = ScreenState,
        parentServiceRegistry = parentRegistrySeen,
    )
}
