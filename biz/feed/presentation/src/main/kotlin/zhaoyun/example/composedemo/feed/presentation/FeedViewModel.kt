package zhaoyun.example.composedemo.feed.presentation

import zhaoyun.example.composedemo.feed.domain.FeedEffect
import zhaoyun.example.composedemo.feed.domain.FeedEvent
import zhaoyun.example.composedemo.feed.domain.FeedState
import zhaoyun.example.composedemo.feed.domain.FeedUseCase
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer

class FeedViewModel(
    useCase: FeedUseCase,
    injectedReducer: Reducer<FeedState>? = null
) : BaseViewModel<FeedState, FeedEvent, FeedEffect>(
    initialState = FeedState(),
    injectedReducer,
    useCase
)
