package zhaoyun.example.composedemo.story.presentation.di

import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.service.feed.api.model.StoryCard
import zhaoyun.example.composedemo.story.presentation.StoryCardViewModel

val storyPresentationModule = module {
    viewModel { (card: StoryCard, parentServiceRegistry: ServiceRegistry) ->
        StoryCardViewModel(card = card, parentServiceRegistry = parentServiceRegistry)
    }
}
