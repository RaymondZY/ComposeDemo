package zhaoyun.example.composedemo.story.input.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.input.domain.InputState
import zhaoyun.example.composedemo.story.input.presentation.InputViewModel

val inputPresentationModule = module {
    scope(named("MviScope")) {
        viewModel { (stateHolder: StateHolder<InputState>) ->
            InputViewModel(stateHolder, get())
        }
    }
}
