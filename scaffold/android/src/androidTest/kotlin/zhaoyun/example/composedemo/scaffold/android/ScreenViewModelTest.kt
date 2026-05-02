package zhaoyun.example.composedemo.scaffold.android

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
import org.koin.core.qualifier.named
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

@RunWith(AndroidJUnit4::class)
class ScreenViewModelTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    companion object {
        private val testModule = module {
            viewModel { (keyData: String) ->
                DefaultScreenViewModel(keyData = keyData)
            }
            viewModel { (payload: ScreenPayload, marker: String) ->
                CustomScreenViewModel(
                    payload = payload,
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
    fun screenViewModel_default_parameters_include_key_data() {
        val koin = org.koin.core.context.GlobalContext.get()
        val scope = koin.createScope("test1", named("MviScreenScope"))
        var viewModel: DefaultScreenViewModel? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalKoinScope provides scope) {
                val resolved: DefaultScreenViewModel = screenViewModel("story")
                SideEffect {
                    viewModel = resolved
                }
            }
        }

        composeRule.runOnIdle {
            assertNotNull(viewModel)
            assertEquals("story", viewModel?.keyData)
        }

        scope.close()
    }

    @Test
    fun screenViewModel_custom_parameters_receive_payload() {
        val koin = org.koin.core.context.GlobalContext.get()
        val scope = koin.createScope("test2", named("MviScreenScope"))
        val payload = ScreenPayload("story")
        var viewModel: CustomScreenViewModel? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalKoinScope provides scope) {
                val resolved: CustomScreenViewModel = screenViewModel(payload) {
                    parametersOf(payload, "custom")
                }
                SideEffect {
                    viewModel = resolved
                }
            }
        }

        composeRule.runOnIdle {
            assertNotNull(viewModel)
            assertEquals(payload, viewModel?.payload)
            assertEquals("custom", viewModel?.marker)
        }

        scope.close()
    }

    @Test
    fun screenViewModel_requires_localKoinScope() {
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
    ) : BaseViewModel<ScreenState, ScreenEvent, ScreenEffect>(
        stateHolder = zhaoyun.example.composedemo.scaffold.core.mvi.StateHolderImpl(ScreenState),
    )

    private class CustomScreenViewModel(
        val payload: ScreenPayload,
        val marker: String,
    ) : BaseViewModel<ScreenState, ScreenEvent, ScreenEffect>(
        stateHolder = zhaoyun.example.composedemo.scaffold.core.mvi.StateHolderImpl(ScreenState),
    )
}
