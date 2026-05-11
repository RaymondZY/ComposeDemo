package zhaoyun.example.composedemo.story.infobar.core

class FakeLikeRepository : LikeRepository {
    override suspend fun toggleLike(cardId: String, isLiked: Boolean, currentLikes: Int): LikeResult {
        val newLikes = if (isLiked) currentLikes + 1 else (currentLikes - 1).coerceAtLeast(0)
        return LikeResult(isLiked = isLiked, likes = newLikes)
    }
}
