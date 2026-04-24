package zhaoyun.example.composedemo.story.message.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.message.domain.MessageState
import zhaoyun.example.composedemo.story.message.presentation.MessageViewModel

val messagePresentationModule = module {
    viewModel { (stateHolder: StateHolder<MessageState>) ->
        MessageViewModel(messageStateHolder = stateHolder)
    }
}
