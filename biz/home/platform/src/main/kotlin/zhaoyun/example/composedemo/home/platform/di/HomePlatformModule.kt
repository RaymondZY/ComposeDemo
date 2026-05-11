package zhaoyun.example.composedemo.home.platform.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.home.platform.HomeViewModel
import zhaoyun.example.composedemo.scaffold.platform.MviKoinScopes

val homePlatformModule = module {
    scope(MviKoinScopes.Screen) {
        viewModel {
            HomeViewModel(get())
        }
    }
}

val homeModules = listOf(
    homePlatformModule
)
