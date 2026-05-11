package zhaoyun.example.composedemo.story.sharepanel.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl

class SharePanelUseCaseTest {

    private fun createUseCase(
        cardId: String = "story-1",
        backgroundImageUrl: String = "https://example.com/bg.jpg",
        shareRepository: ShareRepository = FakeShareRepository(),
    ) = SharePanelUseCase(
        cardId = cardId,
        backgroundImageUrl = backgroundImageUrl,
        shareRepository = shareRepository,
        scope = CoroutineScope(SupervisorJob() + UnconfinedTestDispatcher()),
        stateHolder = SharePanelState(cardId = cardId, backgroundImageUrl = backgroundImageUrl).toStateHolder(),
        serviceRegistry = MutableServiceRegistryImpl(),
    )

    @Test
    fun `打开面板成功获取分享链接`() = runTest {
        val useCase = createUseCase(cardId = "story-1")

        useCase.receiveEvent(SharePanelEvent.OnPanelShown)

        assertEquals(
            SharePanelState(
                cardId = "story-1",
                backgroundImageUrl = "https://example.com/bg.jpg",
                shareLink = "https://example.com/share/story-1",
                isLoadingShareLink = false,
            ),
            useCase.state.value,
        )
    }

    @Test
    fun `打开面板获取分享链接失败时提示且保留面板状态`() = runTest {
        val failingRepository = object : ShareRepository {
            override suspend fun getShareLink(cardId: String): String {
                throw RuntimeException("network error")
            }
        }
        val useCase = createUseCase(shareRepository = failingRepository)

        val baseEffectDeferred = async { useCase.baseEffect.first() }
        useCase.receiveEvent(SharePanelEvent.OnPanelShown)

        assertEquals(BaseEffect.ShowToast("网络失败"), baseEffectDeferred.await())
        assertEquals("", useCase.state.value.shareLink)
        assertEquals(false, useCase.state.value.isLoadingShareLink)
    }

    @Test
    fun `点击保存图片时发送保存当前背景图片效果`() = runTest {
        val useCase = createUseCase(backgroundImageUrl = "https://example.com/current.jpg")

        val effectDeferred = async { useCase.effect.first() }
        useCase.receiveEvent(SharePanelEvent.OnSaveImageClicked)

        assertEquals(
            SharePanelEffect.SaveImageToAlbum("https://example.com/current.jpg"),
            effectDeferred.await(),
        )
    }

    @Test
    fun `背景图片为空时点击保存图片发送提示且不触发保存`() = runTest {
        val useCase = createUseCase(backgroundImageUrl = "")

        val baseEffectDeferred = async { useCase.baseEffect.first() }
        useCase.receiveEvent(SharePanelEvent.OnSaveImageClicked)

        assertEquals(BaseEffect.ShowToast("图片不可用"), baseEffectDeferred.await())
    }

    @Test
    fun `分享链接可用时点击复制链接发送复制效果`() = runTest {
        val useCase = createUseCase()
        useCase.receiveEvent(SharePanelEvent.OnPanelShown)

        val effectDeferred = async { useCase.effect.first() }
        useCase.receiveEvent(SharePanelEvent.OnCopyLinkClicked)

        assertEquals(
            SharePanelEffect.CopyLinkToClipboard("https://example.com/share/story-1"),
            effectDeferred.await(),
        )
    }

    @Test
    fun `分享链接为空时点击复制链接发送提示`() = runTest {
        val useCase = createUseCase()

        val baseEffectDeferred = async { useCase.baseEffect.first() }
        useCase.receiveEvent(SharePanelEvent.OnCopyLinkClicked)

        assertEquals(BaseEffect.ShowToast("分享链接不可用"), baseEffectDeferred.await())
    }

    @Test
    fun `分享链接可用时点击更多发送系统分享效果`() = runTest {
        val useCase = createUseCase()
        useCase.receiveEvent(SharePanelEvent.OnPanelShown)

        val effectDeferred = async { useCase.effect.first() }
        useCase.receiveEvent(SharePanelEvent.OnMoreClicked)

        assertEquals(
            SharePanelEffect.ShareLinkWithSystem("https://example.com/share/story-1"),
            effectDeferred.await(),
        )
    }

    @Test
    fun `分享链接为空时点击更多发送提示`() = runTest {
        val useCase = createUseCase()

        val baseEffectDeferred = async { useCase.baseEffect.first() }
        useCase.receiveEvent(SharePanelEvent.OnMoreClicked)

        assertEquals(BaseEffect.ShowToast("分享链接不可用"), baseEffectDeferred.await())
    }

    @Test
    fun `分享链接请求中重复打开面板不会重复请求`() = runTest {
        val repository = CountingShareRepository(this)
        val useCase = SharePanelUseCase(
            cardId = "story-1",
            backgroundImageUrl = "https://example.com/bg.jpg",
            shareRepository = repository,
            scope = CoroutineScope(SupervisorJob() + coroutineContext),
            stateHolder = SharePanelState(
                cardId = "story-1",
                backgroundImageUrl = "https://example.com/bg.jpg",
            ).toStateHolder(),
            serviceRegistry = MutableServiceRegistryImpl(),
        )

        useCase.receiveEvent(SharePanelEvent.OnPanelShown)
        useCase.receiveEvent(SharePanelEvent.OnPanelShown)
        advanceUntilIdle()

        assertEquals(1, repository.calls)
        assertEquals("https://example.com/share/story-1", useCase.state.value.shareLink)
    }

    private class CountingShareRepository(
        private val scope: TestScope,
    ) : ShareRepository {
        var calls = 0

        override suspend fun getShareLink(cardId: String): String {
            calls += 1
            delay(100)
            return "https://example.com/share/$cardId"
        }
    }
}
