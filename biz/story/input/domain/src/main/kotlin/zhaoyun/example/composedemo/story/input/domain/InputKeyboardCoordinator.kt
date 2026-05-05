package zhaoyun.example.composedemo.story.input.domain

class InputKeyboardCoordinator {
    private val listeners = java.util.WeakHashMap<InputFocusSpi, Unit>()

    @Synchronized
    fun register(spi: InputFocusSpi) {
        listeners[spi] = Unit
    }

    @Synchronized
    fun requestDismiss() {
        listeners.keys.toList().forEach { it.dismissKeyboard() }
    }
}
