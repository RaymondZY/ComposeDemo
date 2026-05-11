package zhaoyun.example.composedemo.story.sharepanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class SharePanelEvent : UiEvent {
    data object OnPanelShown : SharePanelEvent()
    data object OnSaveImageClicked : SharePanelEvent()
    data object OnCopyLinkClicked : SharePanelEvent()
    data object OnMoreClicked : SharePanelEvent()
}
