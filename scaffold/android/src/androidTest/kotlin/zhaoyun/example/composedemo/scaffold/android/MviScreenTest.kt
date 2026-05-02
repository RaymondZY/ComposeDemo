package zhaoyun.example.composedemo.scaffold.android

import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

@RunWith(AndroidJUnit4::class)
class MviScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun mviScreen_exposes_view_model_registry_to_content() {
        val viewModel = ScreenTestViewModel()
        var seenRegistry: Any? = null

        composeRule.setContent {
            MviScreen(viewModel = viewModel) {
                val currentRegistry = LocalServiceRegistry.current
                SideEffect {
                    seenRegistry = currentRegistry
                }
            }
        }

        composeRule.runOnIdle {
            assertSame(viewModel.serviceRegistry, seenRegistry)
        }
    }

    @Test
    fun mviScreen_forwards_base_effects_to_handler() {
        val viewModel = ScreenTestViewModel()
        val receivedEffects = mutableListOf<BaseEffect>()

        composeRule.setContent {
            MviScreen(
                viewModel = viewModel,
                onBaseEffect = { effect ->
                    receivedEffects += effect
                    true
                },
            ) {}
        }

        composeRule.runOnIdle {
            viewModel.sendEvent(ScreenEvent.EmitBaseEffect)
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            receivedEffects.isNotEmpty()
        }

        composeRule.runOnIdle {
            assertEquals(listOf(BaseEffect.ShowDialog("title", "message")), receivedEffects)
        }
    }

    private data class ScreenState(
        val value: Int = 0,
    ) : UiState

    private sealed interface ScreenEvent : UiEvent {
        data object EmitBaseEffect : ScreenEvent
    }

    private data object ScreenEffect : UiEffect

    private class EffectUseCase(
        stateHolder: StateHolder<ScreenState>? = null,
    ) : BaseUseCase<ScreenState, ScreenEvent, ScreenEffect>(
        initialState = ScreenState(),
        stateHolder = stateHolder,
    ) {
        override suspend fun onEvent(event: ScreenEvent) {
            if (event == ScreenEvent.EmitBaseEffect) {
                dispatchBaseEffect(BaseEffect.ShowDialog("title", "message"))
            }
        }
    }

    private class ScreenTestViewModel : BaseViewModel<ScreenState, ScreenEvent, ScreenEffect>(
        initialState = ScreenState(),
        { holder -> EffectUseCase(stateHolder = holder) },
    )
}
