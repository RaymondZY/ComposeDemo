package zhaoyun.example.composedemo

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import zhaoyun.example.composedemo.login.presentation.di.loginModules
import zhaoyun.example.composedemo.service.storage.impl.di.storageModule
import zhaoyun.example.composedemo.service.usercenter.impl.di.userCenterModule
import zhaoyun.example.composedemo.todo.presentation.di.todoModules

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
            modules(userCenterModule + storageModule + loginModules + todoModules)
        }
    }
}
