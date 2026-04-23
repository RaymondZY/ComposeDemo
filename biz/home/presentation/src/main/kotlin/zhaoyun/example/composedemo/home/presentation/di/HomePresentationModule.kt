package zhaoyun.example.composedemo.home.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.home.domain.HomeState
import zhaoyun.example.composedemo.home.domain.homeDomainModule
import zhaoyun.example.composedemo.home.presentation.HomeViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer

val homePresentationModule = module {
    viewModel { params ->
        HomeViewModel(
            get(),
            params.getOrNull<Reducer<HomeState>>()
        )
    }
}

val homeModules = listOf(
    homeDomainModule,
    homePresentationModule
)
