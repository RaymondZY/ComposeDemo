package zhaoyun.example.composedemo.story.input.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class InputKeyboardCoordinator {

    // 当前持焦 InputArea 在 window 根坐标系下的 bounds（用 4 个 Float 避免 domain 依赖 ui Rect）
    data class Bounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float,
    ) {
        fun contains(x: Float, y: Float): Boolean =
            x in left..right && y in top..bottom
    }

    private val listeners = java.util.WeakHashMap<InputFocusSpi, Unit>()

    private val _activeInputBounds = MutableStateFlow<Bounds?>(null)
    val activeInputBounds: StateFlow<Bounds?> get() = _activeInputBounds

    @Synchronized
    fun register(spi: InputFocusSpi) {
        listeners[spi] = Unit
    }

    @Synchronized
    fun requestDismiss() {
        listeners.keys.toList().forEach { it.dismissKeyboard() }
    }

    fun setActiveBounds(bounds: Bounds) {
        _activeInputBounds.value = bounds
    }

    fun clearActiveBounds() {
        _activeInputBounds.value = null
    }
}
