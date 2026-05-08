package zhaoyun.example.composedemo.feed.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class FeedUseCase(
    stateHolder: StateHolder<FeedState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<FeedState, FeedEvent, FeedEffect>(
    stateHolder,
    serviceRegistry,
) {

    override suspend fun onEvent(event: FeedEvent) {
        when (event) {
            is FeedEvent.OnRefreshFailed -> dispatchBaseEffect(BaseEffect.ShowSnackbar("刷新失败，请重试"))
            is FeedEvent.OnLoadMoreFailed -> dispatchBaseEffect(BaseEffect.ShowSnackbar("加载失败，请重试"))
        }
    }
}
