package zhaoyun.example.composedemo.home.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.home.presentation.HomeViewModel
import zhaoyun.example.composedemo.scaffold.android.MviKoinScopes

val homePresentationModule = module {
    scope(MviKoinScopes.Screen) {
        viewModel {
            HomeViewModel(get())
        }
    }
}

val homeModules = listOf(
    homePresentationModule
)
