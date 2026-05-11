package zhaoyun.example.composedemo.story.platform

import org.junit.Assert.assertEquals
import org.junit.Test

class InputImeOffsetTest {

    @Test
    fun `UC10 returns zero while keyboard keeps more than safety margin`() {
        val intrusion = calculateInputImeIntrusion(
            windowHeight = 1000f,
            inputAreaBottom = 700f,
            imeBottom = 250f,
            safetyMarginPx = 10f,
        )

        assertEquals(0f, intrusion, 0.001f)
    }

    @Test
    fun `UC10 moves content by keyboard intrusion into safety margin`() {
        val intrusion = calculateInputImeIntrusion(
            windowHeight = 1000f,
            inputAreaBottom = 700f,
            imeBottom = 320f,
            safetyMarginPx = 10f,
        )

        assertEquals(30f, intrusion, 0.001f)
    }

    @Test
    fun `UC10 ignores offscreen input area measurements`() {
        val aboveWindow = calculateInputImeIntrusion(
            windowHeight = 1000f,
            inputAreaBottom = -20f,
            imeBottom = 500f,
            safetyMarginPx = 10f,
        )
        val belowWindow = calculateInputImeIntrusion(
            windowHeight = 1000f,
            inputAreaBottom = 1200f,
            imeBottom = 500f,
            safetyMarginPx = 10f,
        )

        assertEquals(0f, aboveWindow, 0.001f)
        assertEquals(0f, belowWindow, 0.001f)
    }

    @Test
    fun `UC11 decreases offset as keyboard is dismissed and restores to zero`() {
        val expanded = calculateInputImeIntrusion(
            windowHeight = 1000f,
            inputAreaBottom = 700f,
            imeBottom = 340f,
            safetyMarginPx = 10f,
        )
        val partiallyDismissed = calculateInputImeIntrusion(
            windowHeight = 1000f,
            inputAreaBottom = 700f,
            imeBottom = 315f,
            safetyMarginPx = 10f,
        )
        val dismissed = calculateInputImeIntrusion(
            windowHeight = 1000f,
            inputAreaBottom = 700f,
            imeBottom = 0f,
            safetyMarginPx = 10f,
        )

        assertEquals(50f, expanded, 0.001f)
        assertEquals(25f, partiallyDismissed, 0.001f)
        assertEquals(0f, dismissed, 0.001f)
    }
}
