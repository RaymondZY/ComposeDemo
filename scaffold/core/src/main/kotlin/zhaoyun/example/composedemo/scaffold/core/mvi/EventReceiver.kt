package zhaoyun.example.composedemo.scaffold.core.mvi

interface EventReceiver<E : UiEvent> {
    suspend fun receiveEvent(event: E)
}

class EventReceiverImpl<E : UiEvent>(
    private val onEvent: suspend (E) -> Unit,
) : EventReceiver<E> {
    override suspend fun receiveEvent(event: E) {
        onEvent(event)
    }
}
