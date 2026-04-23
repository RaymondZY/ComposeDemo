package zhaoyun.example.composedemo.story.infobar.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarEffect
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarEvent
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarState
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarUseCase

class InfoBarViewModel(
    private val reducer: Reducer<InfoBarState>,
    cardId: String,
) : BaseViewModel<InfoBarState, InfoBarEvent, InfoBarEffect>(
    InfoBarState(),
    InfoBarUseCase(cardId)
) {
    override fun createReducer(initialState: InfoBarState): Reducer<InfoBarState> = reducer
}
