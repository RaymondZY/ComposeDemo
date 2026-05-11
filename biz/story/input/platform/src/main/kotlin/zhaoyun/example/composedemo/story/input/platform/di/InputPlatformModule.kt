package zhaoyun.example.composedemo.story.input.platform.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.platform.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.input.core.InputState
import zhaoyun.example.composedemo.story.input.platform.InputViewModel

val inputPlatformModule = module {
    scope(MviKoinScopes.Item) {
        viewModel { (stateHolder: StateHolder<InputState>) ->
            InputViewModel(stateHolder, get())
        }
    }
}
