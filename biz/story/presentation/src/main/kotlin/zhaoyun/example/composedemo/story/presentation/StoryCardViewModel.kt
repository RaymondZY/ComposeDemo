package zhaoyun.example.composedemo.story.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.background.domain.BackgroundState
import zhaoyun.example.composedemo.story.domain.StoryCardEffect
import zhaoyun.example.composedemo.story.domain.StoryCardEvent
import zhaoyun.example.composedemo.story.domain.StoryCardState
import zhaoyun.example.composedemo.story.domain.StoryCardUseCase
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarState
import zhaoyun.example.composedemo.story.input.domain.InputState
import zhaoyun.example.composedemo.story.message.domain.MessageState

class StoryCardViewModel(
    stateHolder: StateHolder<StoryCardState>,
) : BaseViewModel<StoryCardState, StoryCardEvent, StoryCardEffect>(
    stateHolder,
    { stateHolder -> StoryCardUseCase(stateHolder) },
) {
    val messageStateHolder: StateHolder<MessageState> by lazy {
        this.stateHolder.derive(StoryCardState::message) {
            copy(message = it)
        }
    }

    val infoBarStateHolder: StateHolder<InfoBarState> by lazy {
        this.stateHolder.derive(StoryCardState::infoBar) {
            copy(infoBar = it)
        }
    }

    val inputStateHolder: StateHolder<InputState> by lazy {
        this.stateHolder.derive(StoryCardState::input) {
            copy(input = it)
        }
    }

    val backgroundStateHolder: StateHolder<BackgroundState> by lazy {
        this.stateHolder.derive(StoryCardState::background) {
            copy(background = it)
        }
    }
}
