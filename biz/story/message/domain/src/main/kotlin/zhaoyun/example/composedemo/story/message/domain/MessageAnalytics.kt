package zhaoyun.example.composedemo.story.message.domain

/**
 * Message 模块提供的埋点分析服务接口。
 *
 * 由 [StoryCardUseCase] 实现并注册到 ServiceRegistry，
 * 供 [MessageUseCase] 等服务消费者发现调用。
 */
interface MessageAnalytics {
    fun trackMessageClicked()
    fun trackMessageExpanded(expanded: Boolean)
}
