package zhaoyun.example.composedemo.feed.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.feed.domain.feedDomainModule
import zhaoyun.example.composedemo.feed.presentation.FeedViewModel

val feedPresentationModule = module {
    viewModel {
        FeedViewModel()
    }
}

val feedModules = listOf(
    feedDomainModule,
    feedPresentationModule
)
