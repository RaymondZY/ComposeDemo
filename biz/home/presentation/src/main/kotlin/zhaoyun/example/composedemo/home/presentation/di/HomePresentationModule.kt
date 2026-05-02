package zhaoyun.example.composedemo.home.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.home.presentation.HomeViewModel

val homePresentationModule = module {
    viewModel {
        HomeViewModel()
    }
}

val homeModules = listOf(
    homePresentationModule
)
