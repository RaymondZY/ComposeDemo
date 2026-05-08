package zhaoyun.example.composedemo.story.infobar.domain

interface ShareRepository {
    suspend fun getShareLink(cardId: String): String
}
