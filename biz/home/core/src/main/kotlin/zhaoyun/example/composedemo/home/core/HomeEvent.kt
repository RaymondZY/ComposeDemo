package zhaoyun.example.composedemo.home.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent

sealed class HomeEvent : UiEvent {
    data class OnTabSelected(val tab: Tab) : HomeEvent()
    data object OnCenterButtonClicked : HomeEvent()
    data class OnBadgeUpdated(val tab: Tab, val badge: TabBadge) : HomeEvent()
}
