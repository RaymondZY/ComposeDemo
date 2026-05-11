package zhaoyun.example.composedemo.feed.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.feed.presentation.FeedViewModel
import zhaoyun.example.composedemo.scaffold.android.MviKoinScopes

val feedPresentationModule = module {
    scope(MviKoinScopes.Screen) {
        viewModel {
            FeedViewModel(get(), get())
        }
    }
}

val feedModules = listOf(
    feedPresentationModule
)
