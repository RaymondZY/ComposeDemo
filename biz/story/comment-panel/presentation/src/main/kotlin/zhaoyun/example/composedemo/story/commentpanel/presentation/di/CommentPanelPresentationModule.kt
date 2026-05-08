package zhaoyun.example.composedemo.story.commentpanel.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.android.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelState
import zhaoyun.example.composedemo.story.commentpanel.presentation.CommentPanelViewModel

val commentPanelPresentationModule = module {
    scope(MviKoinScopes.Item) {
        viewModel { (cardId: String, stateHolder: StateHolder<CommentPanelState>) ->
            CommentPanelViewModel(
                cardId = cardId,
                stateHolder = stateHolder,
                serviceRegistry = get(),
            )
        }
    }
}
