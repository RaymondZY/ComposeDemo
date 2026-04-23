package zhaoyun.example.composedemo.feed.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.feed.domain.feedDomainModule
import zhaoyun.example.composedemo.feed.domain.FeedState
import zhaoyun.example.composedemo.feed.presentation.FeedViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer

val feedPresentationModule = module {
    viewModel { params ->
        FeedViewModel(
            get(),
            params.getOrNull<Reducer<FeedState>>()
        )
    }
}

val feedModules = listOf(
    feedDomainModule,
    feedPresentationModule
)
