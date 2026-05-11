package zhaoyun.example.composedemo.story.commentpanel.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect

sealed class CommentPanelEffect : UiEffect {
    data class NavigateToDialogue(
        val cardId: String,
        val targetId: String,
    ) : CommentPanelEffect()
}
