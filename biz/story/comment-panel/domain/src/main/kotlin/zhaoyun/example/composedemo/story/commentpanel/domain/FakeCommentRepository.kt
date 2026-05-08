package zhaoyun.example.composedemo.story.commentpanel.domain

class FakeCommentRepository : CommentRepository {
    override suspend fun loadComments(cardId: String): List<CommentItem> {
        return listOf(
            CommentItem("comment-1", "小云", "这个故事很有意思"),
            CommentItem("comment-2", "访客", "期待后续"),
        )
    }

    override suspend fun sendComment(cardId: String, content: String): CommentItem {
        return CommentItem("local-$cardId-$content", "我", content)
    }
}
