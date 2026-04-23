package zhaoyun.example.composedemo.story.background.presentation

import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer
import zhaoyun.example.composedemo.story.background.domain.BackgroundEffect
import zhaoyun.example.composedemo.story.background.domain.BackgroundEvent
import zhaoyun.example.composedemo.story.background.domain.BackgroundState
import zhaoyun.example.composedemo.story.background.domain.BackgroundUseCase

class BackgroundViewModel(
    private val backgroundReducer: Reducer<BackgroundState>,
) : BaseViewModel<BackgroundState, BackgroundEvent, BackgroundEffect>(
    BackgroundState(),
    BackgroundUseCase()
) {
    override fun createReducer(initialState: BackgroundState): Reducer<BackgroundState> =
        backgroundReducer
}
