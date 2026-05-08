package zhaoyun.example.composedemo.story.commentpanel.presentation.di

import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import zhaoyun.example.composedemo.scaffold.android.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.story.commentpanel.domain.CommentPanelState
import zhaoyun.example.composedemo.story.commentpanel.presentation.CommentPanelViewModel

class CommentPanelPresentationModuleTest {

    @Test
    fun `comment panel view model resolves from item scope`() {
        val koin = koinApplication {
            modules(commentPanelPresentationModule)
        }.koin
        val scope = koin.createScope("comment-panel", MviKoinScopes.Item).also {
            val registry = MutableServiceRegistryImpl()
            it.declare<MutableServiceRegistryImpl>(
                registry,
                secondaryTypes = listOf(ServiceRegistry::class, MutableServiceRegistry::class),
                allowOverride = true,
            )
        }

        val viewModel = scope.get<CommentPanelViewModel> {
            parametersOf("story-1", CommentPanelState(cardId = "story-1").toStateHolder())
        }

        assertEquals("story-1", viewModel.state.value.cardId)
    }
}
