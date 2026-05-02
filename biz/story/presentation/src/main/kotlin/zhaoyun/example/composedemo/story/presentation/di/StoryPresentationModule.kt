package zhaoyun.example.composedemo.story.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.domain.StoryCardState
import zhaoyun.example.composedemo.story.presentation.StoryCardViewModel

val storyPresentationModule = module {
    viewModel { (stateHolder: StateHolder<StoryCardState>) ->
        StoryCardViewModel(stateHolder)
    }
}
