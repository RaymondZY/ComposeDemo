package zhaoyun.example.composedemo.story.background.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.background.domain.BackgroundState
import zhaoyun.example.composedemo.story.background.presentation.BackgroundViewModel

val backgroundPresentationModule = module {
    viewModel { (stateHolder: StateHolder<BackgroundState>) ->
        BackgroundViewModel(backgroundStateHolder = stateHolder)
    }
}
