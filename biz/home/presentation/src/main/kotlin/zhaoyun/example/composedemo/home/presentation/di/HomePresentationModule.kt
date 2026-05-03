package zhaoyun.example.composedemo.home.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import zhaoyun.example.composedemo.home.presentation.HomeViewModel

val homePresentationModule = module {
    scope(named("MviScreenScope")) {
        viewModel {
            HomeViewModel(get())
        }
    }
}

val homeModules = listOf(
    homePresentationModule
)
