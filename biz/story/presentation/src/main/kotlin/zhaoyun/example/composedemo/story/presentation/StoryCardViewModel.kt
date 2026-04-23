package zhaoyun.example.composedemo.story.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer
import zhaoyun.example.composedemo.story.background.domain.BackgroundState
import zhaoyun.example.composedemo.story.domain.StoryCardEffect
import zhaoyun.example.composedemo.story.domain.StoryCardEvent
import zhaoyun.example.composedemo.story.domain.StoryCardState
import zhaoyun.example.composedemo.story.domain.StoryCardUseCase
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarState
import zhaoyun.example.composedemo.story.input.domain.InputState
import zhaoyun.example.composedemo.story.message.domain.MessageState

class StoryCardViewModel : BaseViewModel<StoryCardState, StoryCardEvent, StoryCardEffect>(
    StoryCardState(),
    null,
    StoryCardUseCase()
) {
    val messageReducer: Reducer<MessageState> by lazy {
        createDelegateReducer(StoryCardState::message) { storyCardState, state ->
            storyCardState.copy(message = state)
        }
    }
    val infoBarReducer: Reducer<InfoBarState> by lazy {
        createDelegateReducer(StoryCardState::infoBar) { storyCardState, state ->
            storyCardState.copy(infoBar = state)
        }
    }
    val inputReducer: Reducer<InputState> by lazy {
        createDelegateReducer(StoryCardState::input) { storyCardState, state ->
            storyCardState.copy(input = state)
        }
    }
    val backgroundReducer: Reducer<BackgroundState> by lazy {
        createDelegateReducer(StoryCardState::background) { storyCardState, state ->
            storyCardState.copy(background = state)
        }
    }
}
