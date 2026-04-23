package zhaoyun.example.composedemo.story.infobar.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer
import zhaoyun.example.composedemo.story.infobar.domain.InfoBarState
import zhaoyun.example.composedemo.story.infobar.presentation.InfoBarViewModel

val infoBarPresentationModule = module {
    viewModel { (reducer: Reducer<InfoBarState>, cardId: String) ->
        InfoBarViewModel(reducer = reducer, cardId = cardId)
    }
}
