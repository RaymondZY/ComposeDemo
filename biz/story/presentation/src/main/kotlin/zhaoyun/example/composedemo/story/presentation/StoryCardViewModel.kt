package zhaoyun.example.composedemo.story.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
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
    parentServiceRegistry: ServiceRegistry? = null,
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
    { stateHolder -> StoryCardUseCase(stateHolder) },
    parentServiceRegistry = parentServiceRegistry,
) {
    val messageStateHolder: StateHolder<MessageState> by lazy {
        stateHolder.derive(StoryCardState::message) {
            copy(message = it)
        }
    }

    val infoBarStateHolder: StateHolder<InfoBarState> by lazy {
        stateHolder.derive(StoryCardState::infoBar) {
            copy(infoBar = it)
        }
    }

    val inputStateHolder: StateHolder<InputState> by lazy {
        stateHolder.derive(StoryCardState::input) {
            copy(input = it)
        }
    }

    val backgroundStateHolder: StateHolder<BackgroundState> by lazy {
        stateHolder.derive(StoryCardState::background) {
            copy(background = it)
        }
    }
}
