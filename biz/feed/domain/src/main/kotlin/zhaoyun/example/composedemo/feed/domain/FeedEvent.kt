package zhaoyun.example.composedemo.feed.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class FeedEvent : UiEvent {
    data object OnRefresh : FeedEvent()
    data object OnLoadMore : FeedEvent()
    data class OnPreload(val index: Int) : FeedEvent()
}
