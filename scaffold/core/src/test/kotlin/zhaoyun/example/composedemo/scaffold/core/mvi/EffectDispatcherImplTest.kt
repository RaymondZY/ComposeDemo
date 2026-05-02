package zhaoyun.example.composedemo.scaffold.core.mvi

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EffectDispatcherImplTest {

    @Test
    fun `dispatches ui effects and base effects to their respective flows`() = runTest {
        val dispatcher = EffectDispatcherImpl<DemoEffect>()
        val effectDeferred = async { dispatcher.effect.first() }
        val baseEffectDeferred = async { dispatcher.baseEffect.first() }

        dispatcher.dispatchEffect(DemoEffect("effect"))
        dispatcher.dispatchBaseEffect(BaseEffect.ShowToast("toast"))

        assertEquals(DemoEffect("effect"), effectDeferred.await())
        assertEquals(BaseEffect.ShowToast("toast"), baseEffectDeferred.await())
    }

    @Test
    fun `buffered channels keep dispatched values until they are collected`() = runTest {
        val dispatcher = EffectDispatcherImpl<DemoEffect>()

        dispatcher.dispatchEffect(DemoEffect("queued"))
        dispatcher.dispatchBaseEffect(BaseEffect.NavigateBack)

        assertEquals(DemoEffect("queued"), dispatcher.effect.first())
        assertEquals(BaseEffect.NavigateBack, dispatcher.baseEffect.first())
    }

    private data class DemoEffect(
        val name: String,
    ) : UiEffect
}
