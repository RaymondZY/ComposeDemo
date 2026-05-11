package zhaoyun.example.composedemo.story.platform

import zhaoyun.example.composedemo.scaffold.platform.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.story.background.core.BackgroundState
import zhaoyun.example.composedemo.story.core.StoryCardEffect
import zhaoyun.example.composedemo.story.core.StoryCardEvent
import zhaoyun.example.composedemo.story.core.StoryCardState
import zhaoyun.example.composedemo.story.core.StoryCardUseCase
import zhaoyun.example.composedemo.story.infobar.core.InfoBarState
import zhaoyun.example.composedemo.story.input.core.InputState
import zhaoyun.example.composedemo.story.message.core.MessageState

class StoryCardViewModel(
    stateHolder: StateHolder<StoryCardState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<StoryCardState, StoryCardEvent, StoryCardEffect>(
    stateHolder,
    serviceRegistry,
    { stateHolder, registry -> StoryCardUseCase(stateHolder, registry) },
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
