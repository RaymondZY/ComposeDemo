package zhaoyun.example.composedemo.home.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase

class HomeUseCase(
    stateHolder: StateHolder<HomeState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<HomeState, HomeEvent, HomeEffect>(
    stateHolder,
    serviceRegistry,
) {

    override suspend fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.OnTabSelected -> handleTabSelected(event.tab)
            is HomeEvent.OnCenterButtonClicked -> { /* no-op */ }
            is HomeEvent.OnBadgeUpdated -> handleBadgeUpdated(event.tab, event.badge)
        }
    }

    private fun handleTabSelected(tab: Tab) {
        if (currentState.selectedTab != tab) {
            updateState { it.copy(selectedTab = tab) }
        }
    }

    private fun handleBadgeUpdated(tab: Tab, badge: TabBadge) {
        updateState {
            it.copy(tabBadges = it.tabBadges.toMutableMap().apply { put(tab, badge) })
        }
    }
}
