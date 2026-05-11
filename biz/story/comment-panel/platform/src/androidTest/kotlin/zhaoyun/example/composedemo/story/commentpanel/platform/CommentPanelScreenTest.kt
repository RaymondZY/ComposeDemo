package zhaoyun.example.composedemo.story.commentpanel.platform

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import zhaoyun.example.composedemo.story.commentpanel.platform.di.commentPanelPlatformModule

class CommentPanelScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun commentPanelScreen_rendersEmptyMviScreen() {
        stopExistingKoin()
        startKoin {
            modules(commentPanelPlatformModule)
        }
        try {
            composeTestRule.setContent {
                CommentPanelScreen(cardId = "story-1")
            }

            composeTestRule.onNodeWithTag(CommentPanelTestTags.EmptyScreen).assertExists()
        } finally {
            stopKoin()
        }
    }

    private fun stopExistingKoin() {
        if (GlobalContext.getOrNull() != null) {
            stopKoin()
        }
    }
}
