package zhaoyun.example.composedemo.story.infobar.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect

sealed class InfoBarEffect : UiEffect {
    data class ShowShareSheet(val cardId: String, val shareLink: String) : InfoBarEffect()
    data class NavigateToComments(val cardId: String) : InfoBarEffect()
    data class ShowHistory(val cardId: String) : InfoBarEffect()
    data class NavigateToStoryDetail(val cardId: String) : InfoBarEffect()
}
