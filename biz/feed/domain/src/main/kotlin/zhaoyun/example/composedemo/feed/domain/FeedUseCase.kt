package zhaoyun.example.composedemo.feed.domain

import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.usecase.BaseUseCase
import zhaoyun.example.composedemo.service.feed.api.FeedRepository

class FeedUseCase(
    private val feedRepository: FeedRepository,
    stateHolder: StateHolder<FeedState>,
    serviceRegistry: MutableServiceRegistry,
) : BaseUseCase<FeedState, FeedEvent, FeedEffect>(
    stateHolder,
    serviceRegistry,
) {

    companion object {
        private const val PAGE_SIZE = 10
        private const val PRELOAD_THRESHOLD = 3
    }

    override suspend fun onEvent(event: FeedEvent) {
        when (event) {
            is FeedEvent.OnRefresh -> refresh()
            is FeedEvent.OnLoadMore -> loadMore()
            is FeedEvent.OnPreload -> preload(event.index)
        }
    }

    private suspend fun refresh() {
        if (currentState.isRefreshing || currentState.isLoading) return
        updateState { it.copy(isRefreshing = true, errorMessage = null) }
        loadFeed(page = 0, isRefresh = true)
    }

    private suspend fun loadMore() {
        val state = currentState
        if (state.isLoading || state.isRefreshing || !state.hasMore) return
        updateState { it.copy(isLoading = true, errorMessage = null) }
        loadFeed(page = state.currentPage, isRefresh = false)
    }

    private suspend fun preload(index: Int) {
        val state = currentState
        if (index >= state.cards.size - PRELOAD_THRESHOLD && state.hasMore && !state.isLoading && !state.isRefreshing) {
            loadMore()
        }
    }

    private suspend fun loadFeed(page: Int, isRefresh: Boolean) {
        feedRepository.fetchFeed(page, PAGE_SIZE)
            .onSuccess { newCards ->
                val updatedCards = if (isRefresh) newCards else currentState.cards + newCards
                val hasMore = newCards.isNotEmpty()
                updateState {
                    it.copy(
                        cards = updatedCards,
                        isLoading = false,
                        isRefreshing = false,
                        currentPage = page + 1,
                        hasMore = hasMore,
                        errorMessage = null
                    )
                }
            }
            .onFailure { _ ->
                if (isRefresh) {
                    updateState { it.copy(isRefreshing = false) }
                    dispatchBaseEffect(BaseEffect.ShowSnackbar("刷新失败，请重试"))
                } else {
                    updateState { it.copy(isLoading = false) }
                    dispatchBaseEffect(BaseEffect.ShowSnackbar("加载失败，请重试"))
                }
            }
    }
}
