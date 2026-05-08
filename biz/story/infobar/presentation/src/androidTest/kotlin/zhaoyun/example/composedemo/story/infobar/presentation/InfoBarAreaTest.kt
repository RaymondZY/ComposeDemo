package zhaoyun.example.composedemo.story.infobar.presentation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarState

class InfoBarAreaTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun author_name_and_handle_are_displayed() {
        setContent(
            InfoBarState(
                storyTitle = "故事标题",
                creatorName = "作者名称",
                creatorHandle = "@author",
            ),
        )

        composeRule.onNodeWithText("作者名称").assertIsDisplayed()
        composeRule.onNodeWithText("@author").assertIsDisplayed()
    }

    @Test
    fun empty_author_handle_is_not_displayed() {
        setContent(
            InfoBarState(
                creatorName = "作者名称",
                creatorHandle = "",
            ),
        )

        composeRule.onNodeWithText("作者名称").assertIsDisplayed()
        composeRule.onAllNodesWithText("@author").assertCountEquals(0)
    }

    private fun setContent(state: InfoBarState) {
        val viewModel = InfoBarViewModel(
            cardId = "story-1",
            stateHolder = state.toStateHolder(),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        composeRule.setContent {
            InfoBarArea(
                viewModel = viewModel,
                cardId = "story-1",
                onSharePanelRequested = {},
            )
        }
    }
}
