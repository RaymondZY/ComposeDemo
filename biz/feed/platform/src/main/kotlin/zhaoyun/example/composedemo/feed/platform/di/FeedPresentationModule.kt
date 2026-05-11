package zhaoyun.example.composedemo.feed.platform.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.feed.platform.FeedViewModel
import zhaoyun.example.composedemo.scaffold.platform.MviKoinScopes

val feedPlatformModule = module {
    scope(MviKoinScopes.Screen) {
        viewModel {
            FeedViewModel(get(), get())
        }
    }
}

val feedModules = listOf(
    feedPlatformModule
)
