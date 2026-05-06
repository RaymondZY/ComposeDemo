package zhaoyun.example.composedemo.story.input.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InputKeyboardCoordinatorTest {

    @Test
    fun `test_coordinator_注册单个SPI后requestDismiss调用一次dismissKeyboard`() {
        val coordinator = InputKeyboardCoordinator()
        var callCount = 0
        val spi = object : InputFocusSpi {
            override fun dismissKeyboard() { callCount++ }
        }
        coordinator.register(spi)
        coordinator.requestDismiss()
        assertEquals(1, callCount)
    }

    @Test
    fun `test_coordinator_注册多个SPI后requestDismiss全部调用`() {
        val coordinator = InputKeyboardCoordinator()
        var callCount = 0
        val spi1 = object : InputFocusSpi { override fun dismissKeyboard() { callCount++ } }
        val spi2 = object : InputFocusSpi { override fun dismissKeyboard() { callCount++ } }
        coordinator.register(spi1)
        coordinator.register(spi2)
        coordinator.requestDismiss()
        assertEquals(2, callCount)
    }

    @Test
    fun `test_coordinator_未注册任何SPI时requestDismiss不崩溃`() {
        val coordinator = InputKeyboardCoordinator()
        coordinator.requestDismiss() // should not throw
    }

    @Test
    fun `test_UC02_active_bounds_can_hit_test_input_region`() {
        val bounds = InputKeyboardCoordinator.Bounds(
            left = 10f,
            top = 20f,
            right = 110f,
            bottom = 70f,
        )

        assertTrue(bounds.contains(10f, 20f))
        assertTrue(bounds.contains(60f, 40f))
        assertTrue(bounds.contains(110f, 70f))
        assertFalse(bounds.contains(9f, 40f))
        assertFalse(bounds.contains(60f, 71f))
    }

    @Test
    fun `test_UC02_active_bounds_are_published_and_cleared`() {
        val coordinator = InputKeyboardCoordinator()
        val bounds = InputKeyboardCoordinator.Bounds(
            left = 10f,
            top = 20f,
            right = 110f,
            bottom = 70f,
        )

        coordinator.setActiveBounds(bounds)
        assertEquals(bounds, coordinator.activeInputBounds.value)

        coordinator.clearActiveBounds()
        assertNull(coordinator.activeInputBounds.value)
    }
}
