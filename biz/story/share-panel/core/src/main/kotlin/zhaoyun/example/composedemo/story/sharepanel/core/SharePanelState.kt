package zhaoyun.example.composedemo.story.sharepanel.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class SharePanelState(
    val cardId: String = "",
    val backgroundImageUrl: String = "",
    val shareLink: String = "",
    val isLoadingShareLink: Boolean = false,
) : UiState
