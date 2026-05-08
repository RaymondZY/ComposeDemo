package zhaoyun.example.composedemo.feed.domain

import androidx.paging.PagingSource
import androidx.paging.PagingState
import zhaoyun.example.composedemo.service.feed.api.FeedRepository
import zhaoyun.example.composedemo.service.feed.api.model.FeedCard

class FeedPagingSource(
    private val feedRepository: FeedRepository,
) : PagingSource<Int, FeedCard>() {

    override fun getRefreshKey(state: PagingState<Int, FeedCard>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, FeedCard> {
        val page = params.key ?: FIRST_PAGE
        return feedRepository.fetchFeed(page, PAGE_SIZE).fold(
            onSuccess = { cards ->
                LoadResult.Page(
                    data = cards,
                    prevKey = if (page == FIRST_PAGE) null else page - 1,
                    nextKey = if (cards.isEmpty()) null else page + 1,
                )
            },
            onFailure = { throwable -> LoadResult.Error(throwable) },
        )
    }

    companion object {
        const val PAGE_SIZE = 10
        const val PRELOAD_DISTANCE = 3
        private const val FIRST_PAGE = 0
    }
}
