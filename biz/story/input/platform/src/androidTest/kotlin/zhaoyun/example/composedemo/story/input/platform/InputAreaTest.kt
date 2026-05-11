package zhaoyun.example.composedemo.story.input.platform

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.story.input.core.InputKeyboardCoordinator
import zhaoyun.example.composedemo.story.input.core.InputState

class InputAreaTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var coordinator: InputKeyboardCoordinator
    private lateinit var viewModel: InputViewModel

    @Before
    fun setup() {
        coordinator = InputKeyboardCoordinator()

        val registry = MutableServiceRegistryImpl()
        registry.register(InputKeyboardCoordinator::class.java, coordinator)
        viewModel = InputViewModel(InputState().toStateHolder(), registry)
    }

    @Test
    fun UC04_collapsed_empty_input_shows_voice_and_plus_buttons() {
        setContent()

        composeRule.onNodeWithTag(InputAreaTestTags.VoiceButton).assertIsDisplayed()
        composeRule.onNodeWithTag(InputAreaTestTags.PlusButton).assertIsDisplayed()
        composeRule.onNodeWithTag(InputAreaTestTags.SendButton).assertDoesNotExist()
        composeRule.onNodeWithTag(InputAreaTestTags.BracketButton).assertDoesNotExist()
    }

    @Test
    fun UC05_focused_input_shows_bracket_button() {
        setContent()

        composeRule.onNodeWithTag(InputAreaTestTags.TextField).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(InputAreaTestTags.BracketButton).assertIsDisplayed()
    }

    @Test
    fun UC09_text_switches_plus_button_to_send_and_clear_restores_plus() {
        setContent()

        composeRule.onNodeWithTag(InputAreaTestTags.TextField).performTextInput("hello")
        composeRule.waitUntil { viewModel.state.value.text == "hello" }

        composeRule.onNodeWithTag(InputAreaTestTags.SendButton).assertIsDisplayed()
        composeRule.onNodeWithTag(InputAreaTestTags.PlusButton).assertDoesNotExist()

        composeRule.onNodeWithTag(InputAreaTestTags.TextField).performTextClearance()
        composeRule.waitUntil { viewModel.state.value.text.isEmpty() }

        composeRule.onNodeWithTag(InputAreaTestTags.PlusButton).assertIsDisplayed()
        composeRule.onNodeWithTag(InputAreaTestTags.SendButton).assertDoesNotExist()
    }

    @Test
    fun UC03_long_input_is_rendered_by_single_line_display_layer() {
        val longText = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
        setContent()

        composeRule.onNodeWithTag(InputAreaTestTags.TextField).performTextInput(longText)
        composeRule.waitUntil { viewModel.state.value.text == longText }

        composeRule.onNodeWithText(longText).assertExists()
    }

    private fun setContent() {
        composeRule.setContent {
            InputArea(
                viewModel = viewModel,
                keyboardCoordinator = coordinator,
            )
        }
    }
}
