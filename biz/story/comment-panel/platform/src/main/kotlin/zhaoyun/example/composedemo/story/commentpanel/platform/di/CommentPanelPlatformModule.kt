package zhaoyun.example.composedemo.story.commentpanel.platform.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.platform.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.commentpanel.core.CommentPanelState
import zhaoyun.example.composedemo.story.commentpanel.platform.CommentPanelViewModel

val commentPanelPlatformModule = module {
    scope(MviKoinScopes.Screen) {
        viewModel { (stateHolder: StateHolder<CommentPanelState>) ->
            CommentPanelViewModel(
                stateHolder = stateHolder,
                serviceRegistry = get(),
            )
        }
    }
}
