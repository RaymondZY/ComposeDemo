package zhaoyun.example.composedemo.story.infobar.platform.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.platform.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.infobar.core.InfoBarState
import zhaoyun.example.composedemo.story.infobar.platform.InfoBarViewModel

val infoBarPlatformModule = module {
    scope(MviKoinScopes.Item) {
        viewModel { (cardId: String, stateHolder: StateHolder<InfoBarState>) ->
            InfoBarViewModel(
                cardId = cardId,
                stateHolder = stateHolder,
                serviceRegistry = get(),
            )
        }
    }
}
