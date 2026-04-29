package zhaoyun.example.composedemo.story.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard
import zhaoyun.example.composedemo.story.background.domain.BackgroundState
import zhaoyun.example.composedemo.story.domain.StoryCardEffect
import zhaoyun.example.composedemo.story.domain.StoryCardEvent
import zhaoyun.example.composedemo.story.domain.StoryCardState
import zhaoyun.example.composedemo.story.domain.StoryCardUseCase
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarState
import zhaoyun.example.composedemo.story.input.domain.InputState
import zhaoyun.example.composedemo.story.message.domain.MessageState

class StoryCardViewModel(
    card: StoryCard,
) : BaseViewModel<StoryCardState, StoryCardEvent, StoryCardEffect>(
    StoryCardState(
        background = BackgroundState(backgroundImageUrl = card.backgroundImageUrl),
        message = MessageState(
            characterName = card.characterName,
            characterSubtitle = card.characterSubtitle ?: "",
            dialogueText = card.dialogueText,
        ),
        infoBar = InfoBarState(
            storyTitle = card.storyTitle,
            creatorName = card.creatorName,
            creatorHandle = card.creatorHandle,
            likes = card.likes,
            shares = card.shares,
            comments = card.comments,
            isLiked = card.isLiked,
        ),
    ),
    null,
    StoryCardUseCase()
) {
    val messageStateHolder: StateHolder<MessageState> by lazy {
        createDelegateStateHolder(StoryCardState::message) { storyCardState, state ->
            storyCardState.copy(message = state)
        }
    }
    val infoBarStateHolder: StateHolder<InfoBarState> by lazy {
        createDelegateStateHolder(StoryCardState::infoBar) { storyCardState, state ->
            storyCardState.copy(infoBar = state)
        }
    }
    val inputStateHolder: StateHolder<InputState> by lazy {
        createDelegateStateHolder(StoryCardState::input) { storyCardState, state ->
            storyCardState.copy(input = state)
        }
    }
    val backgroundStateHolder: StateHolder<BackgroundState> by lazy {
        createDelegateStateHolder(StoryCardState::background) { storyCardState, state ->
            storyCardState.copy(background = state)
        }
    }
}
