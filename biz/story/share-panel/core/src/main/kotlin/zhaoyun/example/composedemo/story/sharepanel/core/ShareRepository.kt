package zhaoyun.example.composedemo.story.sharepanel.core

interface ShareRepository {
    suspend fun getShareLink(cardId: String): String
}
