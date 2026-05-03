package zhaoyun.example.composedemo.feed.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import zhaoyun.example.composedemo.feed.presentation.FeedViewModel

val feedPresentationModule = module {
    scope(named("MviScreenScope")) {
        viewModel {
            FeedViewModel(get(), get())
        }
    }
}

val feedModules = listOf(
    feedPresentationModule
)
