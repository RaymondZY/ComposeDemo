package zhaoyun.example.composedemo.story.sharepanel.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.android.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.sharepanel.domain.SharePanelState
import zhaoyun.example.composedemo.story.sharepanel.presentation.SharePanelViewModel

val sharePanelPresentationModule = module {
    scope(MviKoinScopes.Item) {
        viewModel { (cardId: String, backgroundImageUrl: String, stateHolder: StateHolder<SharePanelState>) ->
            SharePanelViewModel(
                cardId = cardId,
                backgroundImageUrl = backgroundImageUrl,
                stateHolder = stateHolder,
                serviceRegistry = get(),
            )
        }
    }
}
