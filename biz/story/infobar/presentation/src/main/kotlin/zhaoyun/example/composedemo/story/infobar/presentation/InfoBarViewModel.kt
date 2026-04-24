package zhaoyun.example.composedemo.story.infobar.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarEffect
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarEvent
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarState
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarUseCase

class InfoBarViewModel(
    stateHolder: StateHolder<InfoBarState>,
    cardId: String,
) : BaseViewModel<InfoBarState, InfoBarEvent, InfoBarEffect>(
    InfoBarState(),
    stateHolder,
    InfoBarUseCase(cardId)
)
