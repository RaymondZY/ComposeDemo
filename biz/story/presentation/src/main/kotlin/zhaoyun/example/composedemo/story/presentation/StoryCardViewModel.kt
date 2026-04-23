package zhaoyun.example.composedemo.story.presentation

import kotlinx.coroutines.flow.MutableStateFlow
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
    StoryCardUseCase()
) {
    val messageReducer: Reducer<MessageState> by lazy { createMessageReducer() }
    val infoBarReducer: Reducer<InfoBarState> by lazy { createInfoBarReducer() }
    val inputReducer: Reducer<InputState> by lazy { createInputReducer() }
    val backgroundReducer: Reducer<BackgroundState> by lazy { createBackgroundReducer() }

    private fun createMessageReducer(): Reducer<MessageState> {
        val messageStateFlow = MutableStateFlow(state.value.message)
        return createDelegateReducer(
            stateFlow = messageStateFlow,
            onReduce = { transform ->
                val newMessage = transform(state.value.message)
                updateState { it.copy(message = newMessage) }
                messageStateFlow.value = newMessage
            }
        )
    }

    private fun createInfoBarReducer(): Reducer<InfoBarState> {
        val infoBarStateFlow = MutableStateFlow(state.value.infoBar)
        return createDelegateReducer(
            stateFlow = infoBarStateFlow,
            onReduce = { transform ->
                val newInfoBar = transform(state.value.infoBar)
                updateState { it.copy(infoBar = newInfoBar) }
                infoBarStateFlow.value = newInfoBar
            }
        )
    }

    private fun createInputReducer(): Reducer<InputState> {
        val inputStateFlow = MutableStateFlow(state.value.input)
        return createDelegateReducer(
            stateFlow = inputStateFlow,
            onReduce = { transform ->
                val newInput = transform(state.value.input)
                updateState { it.copy(input = newInput) }
                inputStateFlow.value = newInput
            }
        )
    }

    private fun createBackgroundReducer(): Reducer<BackgroundState> {
        val backgroundStateFlow = MutableStateFlow(state.value.background)
        return createDelegateReducer(
            stateFlow = backgroundStateFlow,
            onReduce = { transform ->
                val newBackground = transform(state.value.background)
                updateState { it.copy(background = newBackground) }
                backgroundStateFlow.value = newBackground
            }
        )
    }
}
