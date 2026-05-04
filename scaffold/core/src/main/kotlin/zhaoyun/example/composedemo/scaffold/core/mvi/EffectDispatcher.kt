package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

private const val ENABLE_DEBUG = true
interface EffectDispatcher<F : UiEffect> {
    val effect: Flow<F>
    val baseEffect: Flow<BaseEffect>

    fun dispatchEffect(effect: F)

    fun dispatchBaseEffect(baseEffect: BaseEffect)
}

class EffectDispatcherImpl<F : UiEffect> : EffectDispatcher<F> {
    private val effectChannel = Channel<F>(Channel.BUFFERED)
    private val baseEffectChannel = Channel<BaseEffect>(Channel.BUFFERED)

    override val effect: Flow<F> = effectChannel.receiveAsFlow()

    override val baseEffect: Flow<BaseEffect> = baseEffectChannel.receiveAsFlow()

    override fun dispatchEffect(effect: F) {
        if (ENABLE_DEBUG) {
            println("dispatchEffect: $effect")
        }
        effectChannel.trySend(effect)
    }

    override fun dispatchBaseEffect(baseEffect: BaseEffect) {
        if (ENABLE_DEBUG) {
            println("dispatchBaseEffect: $baseEffect")
        }
        baseEffectChannel.trySend(baseEffect)
    }
}
