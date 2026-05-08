package zhaoyun.example.composedemo.story.sharepanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect

sealed class SharePanelEffect : UiEffect {
    data class SaveImageToAlbum(val imageUrl: String) : SharePanelEffect()
    data class CopyLinkToClipboard(val shareLink: String) : SharePanelEffect()
    data class ShareLinkWithSystem(val shareLink: String) : SharePanelEffect()
}
