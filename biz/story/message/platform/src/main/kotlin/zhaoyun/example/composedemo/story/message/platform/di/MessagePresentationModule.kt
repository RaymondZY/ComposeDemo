package zhaoyun.example.composedemo.story.message.platform.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.platform.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.story.message.core.MessageState
import zhaoyun.example.composedemo.story.message.platform.MessageViewModel

val messagePlatformModule = module {
    scope(MviKoinScopes.Item) {
        viewModel { (stateHolder: StateHolder<MessageState>) ->
            MessageViewModel(
                stateHolder = stateHolder,
                serviceRegistry = get(),
            )
        }
    }
}
