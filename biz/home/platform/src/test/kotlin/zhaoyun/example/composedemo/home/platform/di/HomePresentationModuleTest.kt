package zhaoyun.example.composedemo.home.presentation.di

import org.junit.Assert.assertEquals
import org.junit.Test

class HomePresentationModuleTest {

    @Test
    fun `home modules only expose the presentation module`() {
        assertEquals(listOf(homePresentationModule), homeModules)
    }
}
