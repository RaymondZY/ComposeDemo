package zhaoyun.example.composedemo.story.infobar.domain

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

class InfoBarUseCaseTest {

    private val useCase = InfoBarUseCase(
        cardId = "test-1",
        stateHolder = InfoBarState().toStateHolder(),
        serviceRegistry = MutableServiceRegistryImpl(),
    )

    @Test
    fun `初始状态同步默认值`() {
        val state = useCase.state.value
        assertEquals("", state.storyTitle)
        assertFalse(state.isLiked)
        assertEquals(0, state.likes)
    }

    @Test
    fun `点击点赞切换isLiked并增加likes`() = runTest {
        useCase.receiveEvent(InfoBarEvent.OnLikeClicked)
        assertTrue(useCase.state.value.isLiked)
        assertEquals(1, useCase.state.value.likes)
    }

    @Test
    fun `再次点击点赞恢复原始状态`() = runTest {
        useCase.receiveEvent(InfoBarEvent.OnLikeClicked)
        useCase.receiveEvent(InfoBarEvent.OnLikeClicked)
        assertFalse(useCase.state.value.isLiked)
        assertEquals(0, useCase.state.value.likes)
    }
}
