package zhaoyun.example.composedemo

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import zhaoyun.example.composedemo.feed.platform.di.feedPlatformModule
import zhaoyun.example.composedemo.home.platform.di.homePlatformModule
import zhaoyun.example.composedemo.service.feed.api.FeedRepository
import zhaoyun.example.composedemo.service.feed.mock.FakeFeedRepository
import zhaoyun.example.composedemo.service.storage.impl.di.storageModule
import zhaoyun.example.composedemo.service.usercenter.impl.di.userCenterModule
import zhaoyun.example.composedemo.story.background.platform.di.backgroundPlatformModule
import zhaoyun.example.composedemo.story.commentpanel.platform.di.commentPanelPlatformModule
import zhaoyun.example.composedemo.story.infobar.platform.di.infoBarPlatformModule
import zhaoyun.example.composedemo.story.input.platform.di.inputPlatformModule
import zhaoyun.example.composedemo.story.message.platform.di.messagePlatformModule
import zhaoyun.example.composedemo.story.platform.di.storyPlatformModule
import zhaoyun.example.composedemo.story.sharepanel.platform.di.sharePanelPlatformModule
import zhaoyun.example.composedemo.story.storypanel.platform.di.storyPanelPlatformModule

/**
 * Application 入口 —— 负责初始化 Koin 与服务发现绑定
 *
 * 所有业务模块（:biz、:service）的依赖注入在此组装。
 */
class ComposeDemoApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ComposeDemoApp)
            modules(
                userCenterModule + storageModule +
                listOf(homePlatformModule, feedPlatformModule) +
                listOf(
                    storyPlatformModule,
                    messagePlatformModule,
                    infoBarPlatformModule,
                    commentPanelPlatformModule,
                    sharePanelPlatformModule,
                    inputPlatformModule,
                    backgroundPlatformModule,
                    storyPanelPlatformModule,
                ) +
                listOf(
                    module {
                        single { FakeFeedRepository() } bind FeedRepository::class
                        single { zhaoyun.example.composedemo.story.input.core.InputKeyboardCoordinator() }
                    },
                )
            )
        }
    }
}
