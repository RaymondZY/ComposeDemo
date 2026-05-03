package zhaoyun.example.composedemo.feed.presentation

import zhaoyun.example.composedemo.feed.domain.FeedEffect
import zhaoyun.example.composedemo.feed.domain.FeedEvent
import zhaoyun.example.composedemo.feed.domain.FeedState
import zhaoyun.example.composedemo.feed.domain.FeedUseCase
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.service.feed.api.FeedRepository

class FeedViewModel(
    feedRepository: FeedRepository,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<FeedState, FeedEvent, FeedEffect>(
    FeedState().toStateHolder(),
    serviceRegistry,
    { stateHolder, registry -> FeedUseCase(feedRepository = feedRepository, stateHolder = stateHolder, serviceRegistry = registry) }
)
