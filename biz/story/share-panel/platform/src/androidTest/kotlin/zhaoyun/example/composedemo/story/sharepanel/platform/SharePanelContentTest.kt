package zhaoyun.example.composedemo.story.sharepanel.platform

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class SharePanelContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun default_share_actions_are_displayed() {
        composeRule.setContent {
            SharePanelContent(
                isLoadingShareLink = false,
                onSaveImage = {},
                onCopyLink = {},
                onMore = {},
            )
        }

        composeRule.onNodeWithText("保存图片").assertIsDisplayed()
        composeRule.onNodeWithText("复制链接").assertIsDisplayed()
        composeRule.onNodeWithText("更多").assertIsDisplayed()
    }

    @Test
    fun loading_share_link_text_is_displayed() {
        composeRule.setContent {
            SharePanelContent(
                isLoadingShareLink = true,
                onSaveImage = {},
                onCopyLink = {},
                onMore = {},
            )
        }

        composeRule.onNodeWithText("正在获取分享链接").assertIsDisplayed()
    }
}
