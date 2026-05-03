package zhaoyun.example.composedemo.story.infobar.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarState
import zhaoyun.example.composedemo.story.infobar.presentation.InfoBarViewModel

val infoBarPresentationModule = module {
    scope(named("MviScope")) {
        viewModel { (cardId: String, stateHolder: StateHolder<InfoBarState>) ->
            InfoBarViewModel(
                cardId = cardId,
                stateHolder = stateHolder,
                serviceRegistry = get(),
            )
        }
    }
}
