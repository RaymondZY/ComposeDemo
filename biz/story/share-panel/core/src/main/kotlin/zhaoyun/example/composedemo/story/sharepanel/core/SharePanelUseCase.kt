package zhaoyun.example.composedemo.story.sharepanel.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class SharePanelUseCase(
    private val cardId: String,
    private val backgroundImageUrl: String,
    private val shareRepository: ShareRepository = FakeShareRepository(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    stateHolder: StateHolder<SharePanelState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<SharePanelState, SharePanelEvent, SharePanelEffect>(
    stateHolder,
    serviceRegistry,
) {
    private var shareLinkJob: Job? = null

    override suspend fun onEvent(event: SharePanelEvent) {
        when (event) {
            is SharePanelEvent.OnPanelShown -> loadShareLink()
            is SharePanelEvent.OnSaveImageClicked -> saveImage()
            is SharePanelEvent.OnCopyLinkClicked -> copyLink()
            is SharePanelEvent.OnMoreClicked -> shareMore()
        }
    }

    private fun loadShareLink() {
        if (currentState.isLoadingShareLink) return
        shareLinkJob?.cancel()
        updateState {
            it.copy(
                cardId = cardId,
                backgroundImageUrl = backgroundImageUrl,
                isLoadingShareLink = true,
            )
        }
        shareLinkJob = scope.launch {
            try {
                val link = shareRepository.getShareLink(cardId)
                updateState { it.copy(shareLink = link, isLoadingShareLink = false) }
            } catch (_: Exception) {
                updateState { it.copy(isLoadingShareLink = false) }
                dispatchBaseEffect(BaseEffect.ShowToast("网络失败"))
            }
        }
    }

    private suspend fun saveImage() {
        val imageUrl = currentState.backgroundImageUrl
        if (imageUrl.isBlank()) {
            dispatchBaseEffect(BaseEffect.ShowToast("图片不可用"))
            return
        }
        dispatchEffect(SharePanelEffect.SaveImageToAlbum(imageUrl))
    }

    private suspend fun copyLink() {
        val link = currentState.shareLink
        if (link.isBlank()) {
            dispatchBaseEffect(BaseEffect.ShowToast("分享链接不可用"))
            return
        }
        dispatchEffect(SharePanelEffect.CopyLinkToClipboard(link))
    }

    private suspend fun shareMore() {
        val link = currentState.shareLink
        if (link.isBlank()) {
            dispatchBaseEffect(BaseEffect.ShowToast("分享链接不可用"))
            return
        }
        dispatchEffect(SharePanelEffect.ShareLinkWithSystem(link))
    }
}
