package zhaoyun.example.composedemo.service.feed.api.model

data class StoryCard(
    override val cardId: String,
    override val cardType: String = "story",
    val backgroundImageUrl: String,
    val characterName: String,
    val characterSubtitle: String?,
    val dialogueText: String,
    val storyTitle: String,
    val creatorName: String,
    val creatorHandle: String,
    val likes: Int,
    val shares: Int,
    val comments: Int,
    val isLiked: Boolean = false,
) : FeedCard
