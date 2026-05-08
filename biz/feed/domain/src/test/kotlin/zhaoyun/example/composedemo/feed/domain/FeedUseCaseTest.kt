package zhaoyun.example.composedemo.feed.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

class FeedUseCaseTest {

    @Test
    fun `initial state has no paging error`() {
        val useCase = createUseCase()

        assertNull(useCase.state.value.errorMessage)
    }

    @Test
    fun `refresh failure event shows refresh snackbar`() = runTest {
        val useCase = createUseCase()
        val effect = async { useCase.baseEffect.first() }

        useCase.receiveEvent(FeedEvent.OnRefreshFailed)

        assertEquals(BaseEffect.ShowSnackbar("刷新失败，请重试"), effect.await())
    }

    @Test
    fun `load more failure event shows load more snackbar`() = runTest {
        val useCase = createUseCase()
        val effect = async { useCase.baseEffect.first() }

        useCase.receiveEvent(FeedEvent.OnLoadMoreFailed)

        assertEquals(BaseEffect.ShowSnackbar("加载失败，请重试"), effect.await())
    }

    private fun createUseCase() = FeedUseCase(
        stateHolder = FeedState().toStateHolder(),
        serviceRegistry = MutableServiceRegistryImpl(),
    )
}
