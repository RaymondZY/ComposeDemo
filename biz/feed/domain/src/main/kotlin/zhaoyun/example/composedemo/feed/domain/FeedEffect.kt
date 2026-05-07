package zhaoyun.example.composedemo.feed.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect

sealed class FeedEffect : UiEffect {
    data object ShowRefreshError : FeedEffect()
    data object ShowLoadMoreError : FeedEffect()
}
