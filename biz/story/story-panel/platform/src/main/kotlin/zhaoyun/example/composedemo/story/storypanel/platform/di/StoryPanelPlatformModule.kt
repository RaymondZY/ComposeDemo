package zhaoyun.example.composedemo.story.storypanel.platform.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.platform.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.storypanel.core.StoryPanelState
import zhaoyun.example.composedemo.story.storypanel.platform.StoryPanelViewModel

val storyPanelPlatformModule = module {
    scope(MviKoinScopes.Screen) {
        viewModel { (stateHolder: StateHolder<StoryPanelState>) ->
            StoryPanelViewModel(
                stateHolder = stateHolder,
                serviceRegistry = get(),
            )
        }
    }
}
