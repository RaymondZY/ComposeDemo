package zhaoyun.example.composedemo.story.background.core

import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundStateTest {

    @Test
    fun `initial background url is empty`() {
        assertEquals("", BackgroundState().backgroundImageUrl)
    }

    @Test
    fun `background state can hold story background url`() {
        val state = BackgroundState(backgroundImageUrl = "https://example.com/bg.jpg")

        assertEquals("https://example.com/bg.jpg", state.backgroundImageUrl)
    }

    @Test
    fun `background state can hold empty background url`() {
        val state = BackgroundState(backgroundImageUrl = "")

        assertEquals("", state.backgroundImageUrl)
    }
}
