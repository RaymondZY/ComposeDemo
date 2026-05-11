package zhaoyun.example.composedemo.home.platform.di

import org.junit.Assert.assertEquals
import org.junit.Test

class HomePlatformModuleTest {

    @Test
    fun `home modules only expose the platform module`() {
        assertEquals(listOf(homePlatformModule), homeModules)
    }
}
