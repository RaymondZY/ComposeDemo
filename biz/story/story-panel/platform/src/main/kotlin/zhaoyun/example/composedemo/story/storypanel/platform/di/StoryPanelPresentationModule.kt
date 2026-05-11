package zhaoyun.example.composedemo.story.storypanel.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.android.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.storypanel.domain.StoryPanelState
import zhaoyun.example.composedemo.story.storypanel.presentation.StoryPanelViewModel

val storyPanelPresentationModule = module {
    scope(MviKoinScopes.Screen) {
        viewModel { (stateHolder: StateHolder<StoryPanelState>) ->
            StoryPanelViewModel(
                stateHolder = stateHolder,
                serviceRegistry = get(),
            )
        }
    }
}
