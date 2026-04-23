package zhaoyun.example.composedemo.feed.domain

import org.koin.dsl.module

val feedDomainModule = module {
    factory { FeedUseCase(get()) }
}
