package zhaoyun.example.composedemo

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.bind
import org.koin.dsl.module
import zhaoyun.example.composedemo.feed.presentation.di.feedModules
import zhaoyun.example.composedemo.home.presentation.di.homeModules
import zhaoyun.example.composedemo.service.feed.api.FeedRepository
import zhaoyun.example.composedemo.service.feed.mock.FakeFeedRepository
import zhaoyun.example.composedemo.service.storage.impl.di.storageModule
import zhaoyun.example.composedemo.service.usercenter.impl.di.userCenterModule
import zhaoyun.example.composedemo.story.background.presentation.di.backgroundPresentationModule
import zhaoyun.example.composedemo.story.infobar.presentation.di.infoBarPresentationModule
import zhaoyun.example.composedemo.story.input.presentation.di.inputPresentationModule
import zhaoyun.example.composedemo.story.message.presentation.di.messagePresentationModule
import zhaoyun.example.composedemo.story.presentation.di.storyPresentationModule
import zhaoyun.example.composedemo.story.sharepanel.presentation.di.sharePanelPresentationModule
import zhaoyun.example.composedemo.story.storypanel.presentation.di.storyPanelPresentationModule

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
                homeModules + feedModules +
                listOf(
                    storyPresentationModule,
                    messagePresentationModule,
                    infoBarPresentationModule,
                    sharePanelPresentationModule,
                    inputPresentationModule,
                    backgroundPresentationModule,
                    storyPanelPresentationModule,
                ) +
                listOf(
                    module {
                        single { FakeFeedRepository() } bind FeedRepository::class
                        single { zhaoyun.example.composedemo.story.input.domain.InputKeyboardCoordinator() }
                    },
                )
            )
        }
    }
}
