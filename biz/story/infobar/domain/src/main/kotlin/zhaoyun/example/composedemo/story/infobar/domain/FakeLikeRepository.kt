package zhaoyun.example.composedemo.story.infobar.domain

class FakeLikeRepository : LikeRepository {
    override suspend fun toggleLike(cardId: String, isLiked: Boolean): LikeResult {
        return LikeResult(isLiked = isLiked, likes = if (isLiked) 100 else 0)
    }
}
