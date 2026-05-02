package zhaoyun.example.composedemo.story.infobar.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarState
import zhaoyun.example.composedemo.story.infobar.presentation.InfoBarViewModel

val infoBarPresentationModule = module {
    viewModel { (cardId: String, stateHolder: StateHolder<InfoBarState>, parentServiceRegistry: ServiceRegistry) ->
        InfoBarViewModel(
            cardId = cardId,
            stateHolder = stateHolder,
            parentServiceRegistry = parentServiceRegistry,
        )
    }
}
