package zhaoyun.example.composedemo.story.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard
import zhaoyun.example.composedemo.story.background.core.BackgroundState
import zhaoyun.example.composedemo.story.infobar.core.InfoBarState
import zhaoyun.example.composedemo.story.input.core.InputState
import zhaoyun.example.composedemo.story.message.core.MessageState

data class StoryCardState(
    val background: BackgroundState = BackgroundState(),
    val message: MessageState = MessageState(),
    val infoBar: InfoBarState = InfoBarState(),
    val input: InputState = InputState(),
) : UiState {

    companion object {

        fun from(card: StoryCard): StoryCardState {
            return StoryCardState(
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
            )
        }
    }
}
