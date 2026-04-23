package zhaoyun.example.composedemo.service.feed.mock

import zhaoyun.example.composedemo.service.feed.api.FeedRepository
import zhaoyun.example.composedemo.service.feed.api.model.FeedCard
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard

class FakeFeedRepository : FeedRepository {

    private val mockItems = listOf(
        StoryCard(
            cardId = "1",
            backgroundImageUrl = "https://example.com/cat1.jpg",
            characterName = "橘子",
            characterSubtitle = "猫妈",
            dialogueText = "你们这些四孩子们，今天终于不是觉醒了，是买手机了...",
            storyTitle = "猫之偏心36手...",
            creatorName = "小豆",
            creatorHandle = "@小豆(停更)",
            likes = 1116,
            shares = 8,
            comments = 34,
            isLiked = false,
        ),
        StoryCard(
            cardId = "2",
            backgroundImageUrl = "https://example.com/cat2.jpg",
            characterName = "奶茶",
            characterSubtitle = null,
            dialogueText = "今天天气真好，适合睡觉...",
            storyTitle = "猫咪的日常",
            creatorName = "小王",
            creatorHandle = "@小王",
            likes = 520,
            shares = 12,
            comments = 56,
            isLiked = true,
        ),
    )

    override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
        val start = page * pageSize
        val end = minOf(start + pageSize, mockItems.size)
        return if (start < mockItems.size) {
            Result.success(mockItems.subList(start, end))
        } else {
            Result.success(emptyList())
        }
    }
}
