package zhaoyun.example.composedemo.story.infobar.domain

interface LikeRepository {
    suspend fun toggleLike(cardId: String, isLiked: Boolean, currentLikes: Int): LikeResult
}

data class LikeResult(
    val isLiked: Boolean,
    val likes: Int,
)
