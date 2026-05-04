package zhaoyun.example.composedemo.scaffold.core.mvi

private const val ENABLE_DEBUG = true

interface EventReceiver<E : UiEvent> {
    suspend fun receiveEvent(event: E)
}

class EventReceiverImpl<E : UiEvent>(
    private val onEvent: suspend (E) -> Unit,
) : EventReceiver<E> {
    override suspend fun receiveEvent(event: E) {
        if (ENABLE_DEBUG) {
            println("receiveEvent: $event")
        }
        onEvent(event)
    }
}
