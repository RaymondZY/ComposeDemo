package zhaoyun.example.composedemo.feed.platform

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import zhaoyun.example.composedemo.feed.core.FeedPagingSource
import zhaoyun.example.composedemo.feed.core.FeedEffect
import zhaoyun.example.composedemo.feed.core.FeedEvent
import zhaoyun.example.composedemo.feed.core.FeedState
import zhaoyun.example.composedemo.feed.core.FeedUseCase
import zhaoyun.example.composedemo.scaffold.platform.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.service.feed.api.FeedRepository

class FeedViewModel(
    feedRepository: FeedRepository,
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<FeedState, FeedEvent, FeedEffect>(
    FeedState().toStateHolder(),
    serviceRegistry,
    { stateHolder, registry -> FeedUseCase(stateHolder = stateHolder, serviceRegistry = registry) },
) {
    val pagingData = Pager(
        config = PagingConfig(
            pageSize = FeedPagingSource.PAGE_SIZE,
            initialLoadSize = FeedPagingSource.PAGE_SIZE,
            prefetchDistance = FeedPagingSource.PRELOAD_DISTANCE,
            enablePlaceholders = false,
        ),
        pagingSourceFactory = { FeedPagingSource(feedRepository) },
    ).flow.cachedIn(viewModelScope)
}
