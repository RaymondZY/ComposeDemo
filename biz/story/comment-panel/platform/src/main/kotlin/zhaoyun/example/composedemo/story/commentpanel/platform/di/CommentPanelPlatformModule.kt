package zhaoyun.example.composedemo.story.commentpanel.platform.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.platform.MviKoinScopes
import zhaoyun.example.composedemo.story.commentpanel.core.CommentPanelState
import zhaoyun.example.composedemo.story.commentpanel.platform.CommentPanelViewModel

val commentPanelPlatformModule = module {
    scope(MviKoinScopes.Screen) {
        viewModel { (stateHolder: StateHolder<CommentPanelState>) ->
            createCommentPanelViewModel(
                stateHolder = stateHolder,
                serviceRegistry = get(),
            )
        }
    }
    scope(MviKoinScopes.Item) {
        viewModel { (stateHolder: StateHolder<CommentPanelState>) ->
            createCommentPanelViewModel(
                stateHolder = stateHolder,
                serviceRegistry = get(),
            )
        }
    }
}

private fun createCommentPanelViewModel(
    stateHolder: StateHolder<CommentPanelState>,
    serviceRegistry: MutableServiceRegistry,
): CommentPanelViewModel {
    return CommentPanelViewModel(
        stateHolder = stateHolder,
        serviceRegistry = serviceRegistry,
    )
}
