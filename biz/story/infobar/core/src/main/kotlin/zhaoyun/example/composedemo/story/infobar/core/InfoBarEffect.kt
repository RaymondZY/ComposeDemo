package zhaoyun.example.composedemo.story.infobar.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect

sealed class InfoBarEffect : UiEffect {
    data class OpenSharePanel(val cardId: String) : InfoBarEffect()
    data class OpenCommentPanel(val cardId: String) : InfoBarEffect()
    data class ShowHistory(val cardId: String) : InfoBarEffect()
    data class NavigateToStoryDetail(val cardId: String) : InfoBarEffect()
}
