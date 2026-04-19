package zhaoyun.example.composedemo.service.usercenter.impl.di

import org.koin.dsl.module
import zhaoyun.example.composedemo.service.usercenter.api.UserRepository
import zhaoyun.example.composedemo.service.usercenter.impl.MockUserRepository

/**
 * 用户中心 Koin Module —— 将 API 绑定到具体实现
 */
val userCenterModule = module {
    single<UserRepository> { MockUserRepository() }
}
