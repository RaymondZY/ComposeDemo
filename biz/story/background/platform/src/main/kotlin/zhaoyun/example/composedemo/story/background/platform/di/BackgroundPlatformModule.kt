package zhaoyun.example.composedemo.story.background.platform.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.platform.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.background.core.BackgroundState
import zhaoyun.example.composedemo.story.background.platform.BackgroundViewModel

val backgroundPlatformModule = module {
    scope(MviKoinScopes.Item) {
        viewModel { (stateHolder: StateHolder<BackgroundState>) ->
            BackgroundViewModel(
                stateHolder = stateHolder,
                serviceRegistry = get(),
            )
        }
    }
}
