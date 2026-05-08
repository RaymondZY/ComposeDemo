package zhaoyun.example.composedemo.story.commentpanel.domain

interface CommentRepository {
    suspend fun loadComments(cardId: String): List<CommentItem>

    suspend fun sendComment(cardId: String, content: String): CommentItem
}
