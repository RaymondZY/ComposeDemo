package zhaoyun.example.composedemo.story.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard

class StoryCardStateTest {

    @Test
    fun `from maps story card data into child states`() {
        val state = StoryCardState.from(sampleCard())

        assertEquals("https://example.com/bg.jpg", state.background.backgroundImageUrl)
        assertEquals("Hero", state.message.characterName)
        assertEquals("Guide", state.message.characterSubtitle)
        assertEquals("Hello", state.message.dialogueText)
        assertEquals("Story", state.infoBar.storyTitle)
        assertEquals("Creator", state.infoBar.creatorName)
        assertEquals("@creator", state.infoBar.creatorHandle)
        assertEquals(7, state.infoBar.likes)
        assertEquals(3, state.infoBar.shares)
        assertEquals(5, state.infoBar.comments)
        assertTrue(state.infoBar.isLiked)
        assertEquals("", state.input.text)
        assertFalse(state.input.isFocused)
    }

    @Test
    fun `from uses empty subtitle when story card subtitle is absent`() {
        val state = StoryCardState.from(sampleCard(characterSubtitle = null))

        assertEquals("", state.message.characterSubtitle)
        assertEquals("Hero", state.message.characterName)
        assertEquals("Hello", state.message.dialogueText)
    }

    @Test
    fun `updating one child state keeps other child states unchanged`() {
        val initial = StoryCardState.from(sampleCard())
        val updated = initial.copy(
            message = initial.message.copy(isExpanded = true),
        )

        assertTrue(updated.message.isExpanded)
        assertEquals(initial.background, updated.background)
        assertEquals(initial.infoBar, updated.infoBar)
        assertEquals(initial.input, updated.input)
    }

    private fun sampleCard(
        characterSubtitle: String? = "Guide",
    ): StoryCard {
        return StoryCard(
            cardId = "card-1",
            backgroundImageUrl = "https://example.com/bg.jpg",
            characterName = "Hero",
            characterSubtitle = characterSubtitle,
            dialogueText = "Hello",
            storyTitle = "Story",
            creatorName = "Creator",
            creatorHandle = "@creator",
            likes = 7,
            shares = 3,
            comments = 5,
            isLiked = true,
        )
    }
}
