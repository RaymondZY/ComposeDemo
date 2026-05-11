package zhaoyun.example.composedemo.story.sharepanel.core

class FakeShareRepository : ShareRepository {
    override suspend fun getShareLink(cardId: String): String {
        return "https://example.com/share/$cardId"
    }
}
