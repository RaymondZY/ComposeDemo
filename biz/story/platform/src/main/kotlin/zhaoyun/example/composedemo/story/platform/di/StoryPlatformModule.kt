package zhaoyun.example.composedemo.story.platform.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.platform.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.core.StoryCardState
import zhaoyun.example.composedemo.story.platform.StoryCardViewModel

val storyPlatformModule = module {
    scope(MviKoinScopes.Item) {
        viewModel { (stateHolder: StateHolder<StoryCardState>) ->
            StoryCardViewModel(stateHolder, get())
        }
    }
}
