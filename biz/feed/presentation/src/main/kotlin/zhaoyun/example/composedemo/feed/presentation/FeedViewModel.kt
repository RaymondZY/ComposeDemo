package zhaoyun.example.composedemo.feed.presentation

import org.koin.core.context.GlobalContext.get
import zhaoyun.example.composedemo.feed.domain.FeedEffect
import zhaoyun.example.composedemo.feed.domain.FeedEvent
import zhaoyun.example.composedemo.feed.domain.FeedState
import zhaoyun.example.composedemo.feed.domain.FeedUseCase
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel

class FeedViewModel : BaseViewModel<FeedState, FeedEvent, FeedEffect>(
    FeedState(),
    { stateHolder -> FeedUseCase(feedRepository = get().get(), stateHolder = stateHolder) }
)
