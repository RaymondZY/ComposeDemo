package zhaoyun.example.composedemo.story.input.domain

import org.junit.Assert.assertEquals
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
}
