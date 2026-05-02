package zhaoyun.example.composedemo.story.presentation

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard
import zhaoyun.example.composedemo.story.message.domain.MessageEvent
import zhaoyun.example.composedemo.story.message.presentation.MessageViewModel

class StoryCardServiceRegistryScopeTest {

    @Test
    fun `message view model can resolve services from the parent story scope`() = runTest {
        val storyCardViewModel = StoryCardViewModel(sampleCard())
        val messageViewModel = MessageViewModel(
            stateHolder = storyCardViewModel.messageStateHolder,
            parentServiceRegistry = storyCardViewModel.serviceRegistry,
        )

        messageViewModel.receiveEvent(MessageEvent.OnDialogueClicked)

        assertTrue(messageViewModel.state.value.isExpanded)
    }

    private fun sampleCard(): StoryCard {
        return StoryCard(
            cardId = "card-1",
            backgroundImageUrl = "https://example.com/background.jpg",
            characterName = "Hero",
            characterSubtitle = "Guide",
            dialogueText = "Hello",
            storyTitle = "Story",
            creatorName = "Creator",
            creatorHandle = "@creator",
            likes = 7,
            shares = 3,
            comments = 5,
            isLiked = false,
        )
    }
}
