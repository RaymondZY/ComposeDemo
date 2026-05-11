package zhaoyun.example.composedemo.feed.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class FeedEvent : UiEvent {
    data object OnRefreshFailed : FeedEvent()
    data object OnLoadMoreFailed : FeedEvent()
}
