package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

/**
 * MviScreen与Reducer架构集成测试
 */
@RunWith(AndroidJUnit4::class)
class MviScreenReducerIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    data class TestState(val text: String) : UiState
    data class TestEvent(val newText: String) : UiEvent
    object TestEffect : UiEffect

    class TestUseCase : BaseUseCase<TestState, TestEvent, TestEffect>(TestState("initial")) {
        override suspend fun onEvent(event: TestEvent) {
            updateState { it.copy(text = event.newText) }
        }
    }

    class LocalTestViewModel : BaseViewModel<TestState, TestEvent, TestEffect>(
        initialState = TestState("initial"),
        TestUseCase()
    )

    class DelegateTestViewModel(private val injectedReducer: Reducer<TestState>) : BaseViewModel<TestState, TestEvent, TestEffect>(
        initialState = TestState("initial"),
        TestUseCase()
    ) {
        override fun createReducer(initialState: TestState): Reducer<TestState> = injectedReducer
    }

    @Composable
    fun TestScreen(state: TestState, onEvent: (TestEvent) -> Unit) {
        BasicText(text = state.text)
    }

    @Test
    fun `LocalReducer模式下MviScreen正确收集State`() {
        val viewModel = LocalTestViewModel()

        composeTestRule.setContent {
            MviScreen(viewModel = viewModel) { state, onEvent ->
                TestScreen(state = state, onEvent = onEvent)
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("initial").assertIsDisplayed()

        viewModel.onEvent(TestEvent("updated"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("updated").assertIsDisplayed()
    }

    @Test
    fun `DelegateReducer模式下MviScreen正确收集State`() {
        val externalState = MutableStateFlow(TestState("external"))
        val reducer = BaseViewModel.createDelegateReducer(
            stateFlow = externalState,
            onReduce = { transform -> externalState.value = transform(externalState.value) }
        )
        val viewModel = DelegateTestViewModel(reducer)

        composeTestRule.setContent {
            MviScreen(viewModel = viewModel) { state, onEvent ->
                TestScreen(state = state, onEvent = onEvent)
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("external").assertIsDisplayed()

        viewModel.onEvent(TestEvent("delegated"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("delegated").assertIsDisplayed()
    }
}
