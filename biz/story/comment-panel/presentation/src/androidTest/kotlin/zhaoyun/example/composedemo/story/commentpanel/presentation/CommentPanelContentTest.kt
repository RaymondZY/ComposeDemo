package zhaoyun.example.composedemo.story.commentpanel.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import zhaoyun.example.composedemo.story.commentpanel.domain.CommentItem
import zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelState

class CommentPanelContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun comments_are_displayed() {
        composeRule.setContent {
            CommentPanelContent(
                state = CommentPanelState(
                    comments = listOf(CommentItem("1", "小云", "评论内容")),
                ),
                onInputChanged = {},
                onSendClicked = {},
            )
        }

        composeRule.onNodeWithText("小云").assertIsDisplayed()
        composeRule.onNodeWithText("评论内容").assertIsDisplayed()
        composeRule.onNodeWithText("写下你的评论").assertIsDisplayed()
        composeRule.onNodeWithText("发送").assertIsDisplayed()
    }

    @Test
    fun empty_comments_show_empty_message() {
        composeRule.setContent {
            CommentPanelContent(
                state = CommentPanelState(comments = emptyList(), isLoadingComments = false),
                onInputChanged = {},
                onSendClicked = {},
            )
        }

        composeRule.onNodeWithText("暂无评论").assertIsDisplayed()
    }

    @Test
    fun loading_comments_show_loading_message() {
        composeRule.setContent {
            CommentPanelContent(
                state = CommentPanelState(isLoadingComments = true),
                onInputChanged = {},
                onSendClicked = {},
            )
        }

        composeRule.onNodeWithText("评论加载中").assertIsDisplayed()
    }
}
