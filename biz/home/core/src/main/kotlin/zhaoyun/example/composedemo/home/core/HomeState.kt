package zhaoyun.example.composedemo.home.core

import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

data class HomeState(
    val selectedTab: Tab = Tab.HOME,
    val tabBadges: Map<Tab, TabBadge> = emptyMap()
) : UiState

data class TabBadge(
    val showRedDot: Boolean = false,
    val unreadCount: Int = 0,
    val hasBadge: Boolean = false
)

enum class Tab {
    HOME,
    DISCOVER,
    MESSAGE,
    PROFILE
}
