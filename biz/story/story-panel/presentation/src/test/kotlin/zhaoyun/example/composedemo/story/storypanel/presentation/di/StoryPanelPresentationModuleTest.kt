package zhaoyun.example.composedemo.story.storypanel.presentation.di

import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import zhaoyun.example.composedemo.scaffold.android.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.story.storypanel.domain.StoryPanelState
import zhaoyun.example.composedemo.story.storypanel.presentation.StoryPanelViewModel

class StoryPanelPresentationModuleTest {

    @Test
    fun `story panel view model resolves from screen scope`() {
        val koin = koinApplication {
            modules(storyPanelPresentationModule)
        }.koin
        val scope = koin.createScope("story-panel", MviKoinScopes.Screen).also {
            val registry = MutableServiceRegistryImpl()
            it.declare<MutableServiceRegistryImpl>(
                registry,
                secondaryTypes = listOf(ServiceRegistry::class, MutableServiceRegistry::class),
                allowOverride = true,
            )
        }

        val viewModel = scope.get<StoryPanelViewModel> {
            parametersOf(StoryPanelState(cardId = "story-1").toStateHolder())
        }

        assertEquals("story-1", viewModel.state.value.cardId)
    }
}
