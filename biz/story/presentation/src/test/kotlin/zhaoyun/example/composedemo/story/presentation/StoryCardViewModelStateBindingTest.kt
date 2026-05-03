package zhaoyun.example.composedemo.story.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard
import zhaoyun.example.composedemo.story.domain.StoryCardState
import zhaoyun.example.composedemo.story.message.presentation.MessageViewModel

class StoryCardViewModelStateBindingTest {

    @Test
    fun `message view model writes through the derived state holder passed at construction`() {
        val parentViewModel = StoryCardViewModel(
            StoryCardState.from(sampleCard()).toStateHolder(),
            MutableServiceRegistryImpl(),
        )
        val messageViewModel = MessageViewModel(
            stateHolder = parentViewModel.messageStateHolder,
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        assertEquals("Hero", messageViewModel.state.value.characterName)

        messageViewModel.updateState { it.copy(isExpanded = true) }

        assertTrue(messageViewModel.state.value.isExpanded)
        assertTrue(parentViewModel.state.value.message.isExpanded)
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
