package zhaoyun.example.composedemo.service.feed.api

import zhaoyun.example.composedemo.service.feed.api.model.FeedCard

interface FeedRepository {
    suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>>
}
