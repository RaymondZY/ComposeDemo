package zhaoyun.example.composedemo.story.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.story.background.domain.BackgroundState
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarState
import zhaoyun.example.composedemo.story.input.domain.InputState
import zhaoyun.example.composedemo.story.message.domain.MessageState

data class StoryCardState(
    val background: BackgroundState = BackgroundState(),
    val message: MessageState = MessageState(),
    val infoBar: InfoBarState = InfoBarState(),
    val input: InputState = InputState(),
) : UiState
