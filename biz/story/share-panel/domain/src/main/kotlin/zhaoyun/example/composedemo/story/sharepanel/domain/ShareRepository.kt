package zhaoyun.example.composedemo.story.sharepanel.domain

interface ShareRepository {
    suspend fun getShareLink(cardId: String): String
}
