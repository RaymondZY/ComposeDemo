package zhaoyun.example.composedemo.story.sharepanel.platform.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.platform.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.sharepanel.core.SharePanelState
import zhaoyun.example.composedemo.story.sharepanel.platform.SharePanelViewModel

val sharePanelPlatformModule = module {
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
