package zhaoyun.example.composedemo.service.feed.mock

import kotlinx.coroutines.delay
import zhaoyun.example.composedemo.service.feed.api.FeedRepository
import zhaoyun.example.composedemo.service.feed.api.model.FeedCard
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard
import kotlin.random.Random

class FakeFeedRepository(
    private val random: Random = Random.Default,
    private val delayRangeMillis: LongRange = 300L..1400L,
    private val failureRate: Double = 0.1,
) : FeedRepository {

    override suspend fun fetchFeed(page: Int, pageSize: Int): Result<List<FeedCard>> {
        require(page >= 0) { "page must be non-negative" }
        require(pageSize >= 0) { "pageSize must be non-negative" }
        require(failureRate in 0.0..1.0) { "failureRate must be in 0.0..1.0" }

        delay(randomDelayMillis())
        if (random.nextDouble() < failureRate) {
            return Result.failure(IllegalStateException("Fake feed request failed"))
        }

        return Result.success(
            List(pageSize) { index ->
                createStoryCard(page = page, index = index)
            },
        )
    }

    private fun createStoryCard(page: Int, index: Int): StoryCard {
        val character = characters.random()
        val dialogue = dialogues.random()
        val title = titles.random()
        val creator = creators.random()
        val seed = backgroundSeeds.random()
        val serial = "${page}_${index}_${random.nextInt(100_000)}"
        return StoryCard(
            cardId = "story_$serial",
            backgroundImageUrl = "https://picsum.photos/seed/$seed-$serial/1080/1920",
            characterName = character.name,
            characterSubtitle = character.subtitle,
            dialogueText = dialogue,
            storyTitle = title,
            creatorName = creator.name,
            creatorHandle = creator.handle,
            likes = random.nextInt(120, 8_800),
            shares = random.nextInt(0, 1_200),
            comments = random.nextInt(0, 600),
            isLiked = random.nextBoolean(),
        )
    }

    private fun randomDelayMillis(): Long {
        require(delayRangeMillis.first >= 0L) { "delayRangeMillis must be non-negative" }
        require(delayRangeMillis.first <= delayRangeMillis.last) { "delayRangeMillis must not be empty" }
        if (delayRangeMillis.first == delayRangeMillis.last) return delayRangeMillis.first
        return random.nextLong(delayRangeMillis.first, delayRangeMillis.last + 1)
    }

    private fun <T> List<T>.random(): T = this[random.nextInt(size)]

    private data class CharacterProfile(
        val name: String,
        val subtitle: String?,
    )

    private data class CreatorProfile(
        val name: String,
        val handle: String,
    )

    private companion object {
        val characters = listOf(
            CharacterProfile("橘子", "猫妈"),
            CharacterProfile("奶茶", null),
            CharacterProfile("旺财", "忠诚卫士"),
            CharacterProfile("赛博剑客", "夜之城传说"),
            CharacterProfile("林间精灵", "自然守护者"),
            CharacterProfile("星际旅人", "流浪者"),
            CharacterProfile("美食家老张", "米其林三星评论家"),
            CharacterProfile("音符少女", "独立音乐人"),
            CharacterProfile("跑鞋大叔", "马拉松破三选手"),
            CharacterProfile("书虫阿明", "年阅读量100+"),
            CharacterProfile("代码诗人", "全栈工程师"),
            CharacterProfile("背包客小林", "环游世界中"),
            CharacterProfile("咖啡师老陈", "手冲十年"),
            CharacterProfile("画中人", "油画系研究生"),
            CharacterProfile("海的女儿", "潜水教练"),
        )

        val dialogues = listOf(
            "你们今天终于不是觉醒了，是买手机了！一个个成天抱着手机刷短视频，饭也不吃觉也不睡。",
            "今天天气真好，阳光明媚，微风不燥，正是睡觉的好时候。",
            "主人主人！该出门了！外面的风在呼唤我，草地上的每一根草都在邀请我去踩一踩。",
            "在这个城市，要么成为传奇，要么成为灰烬。",
            "每一片落叶都是森林写给大地的情书。",
            "我们在星海中相遇，又在星海中告别。",
            "这道菜的关键在于火候，多一秒则老，少一秒则生。",
            "这首歌写给我所有失眠的夜晚。",
            "第42公里，腿已经不是自己的了，但心还在跑。",
            "这本书读到最后一页，我哭了整整一个小时。",
            "写代码和写诗本质上是一样的，都是在用文字创造世界。",
            "在西藏的星空下，我第一次感受到了人类的渺小。",
            "一杯好的手冲，温度要控制在90度，水流要稳。",
            "这幅画我画了三个月，每一笔都是我对这个世界的理解。",
            "海底的世界比陆地上安静一万倍。",
            "如果今天必须重新开始，那我希望从一杯热咖啡和一段安静的路开始。",
            "故事不是发生在远方，而是发生在你终于愿意抬头看的那一刻。",
        )

        val titles = listOf(
            "猫之偏心36手",
            "猫咪的日常",
            "狗狗日记第1024篇",
            "霓虹下的刀锋",
            "森之语",
            "星际漂流瓶",
            "老张探店·第88期",
            "凌晨三点的旋律",
            "马拉松日记：终点前的独白",
            "那些让我哭湿枕头的书",
            "程序员的浪漫",
            "一个人的朝圣",
            "老陈的咖啡课",
            "画布上的时间",
            "深蓝之下",
            "今天重新开始",
            "抬头看见的故事",
        )

        val creators = listOf(
            CreatorProfile("小豆", "@小豆"),
            CreatorProfile("小王", "@小王"),
            CreatorProfile("阿强", "@阿强遛狗中"),
            CreatorProfile("夜行者", "@NightWalker2077"),
            CreatorProfile("绿野", "@GreenField"),
            CreatorProfile("星云", "@NebulaWander"),
            CreatorProfile("吃遍天下", "@FoodieZhang"),
            CreatorProfile("小夜曲", "@NocturneGirl"),
            CreatorProfile("奔跑吧", "@RunForLife"),
            CreatorProfile("阅读者", "@BookWormMing"),
            CreatorProfile("HelloWorld", "@CodePoet"),
            CreatorProfile("在路上", "@BackpackerLin"),
            CreatorProfile("咖啡因", "@CoffeeChen"),
            CreatorProfile("艺术学院", "@ArtSoul"),
            CreatorProfile("海洋之心", "@OceanHeart"),
        )

        val backgroundSeeds = listOf(
            "cat1",
            "cat2",
            "dog1",
            "city1",
            "forest1",
            "space1",
            "food1",
            "music1",
            "sport1",
            "book1",
            "tech1",
            "travel1",
            "coffee1",
            "art1",
            "sea1",
        )
    }
}
