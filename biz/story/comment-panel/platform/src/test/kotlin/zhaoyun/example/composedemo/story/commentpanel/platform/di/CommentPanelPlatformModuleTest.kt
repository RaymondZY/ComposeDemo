package zhaoyun.example.composedemo.story.commentpanel.platform.di

import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.dsl.koinApplication
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.scaffold.platform.MviKoinScopes
import zhaoyun.example.composedemo.story.commentpanel.core.CommentPanelState
import zhaoyun.example.composedemo.story.commentpanel.platform.CommentPanelViewModel

class CommentPanelPlatformModuleTest {

    @Test
    fun `comment panel view model resolves from screen scope`() {
        val koin = koinApplication {
            modules(commentPanelPlatformModule)
        }.koin
        val scope = koin.createScopeWithRegistry("comment-panel", MviKoinScopes.Screen)

        try {
            val viewModel = scope.get<CommentPanelViewModel> {
                parametersOf(CommentPanelState(cardId = "story-1").toStateHolder())
            }

            assertEquals("story-1", viewModel.state.value.cardId)
        } finally {
            scope.close()
        }
    }

    @Test
    fun `comment panel view model resolves from item scope`() {
        val koin = koinApplication {
            modules(commentPanelPlatformModule)
        }.koin
        val scope = koin.createScopeWithRegistry("comment-panel-item", MviKoinScopes.Item)

        try {
            val viewModel = scope.get<CommentPanelViewModel> {
                parametersOf(CommentPanelState(cardId = "story-1").toStateHolder())
            }

            assertEquals("story-1", viewModel.state.value.cardId)
        } finally {
            scope.close()
        }
    }

    private fun org.koin.core.Koin.createScopeWithRegistry(
        scopeId: String,
        qualifier: Qualifier,
    ): Scope {
        return createScope(scopeId, qualifier).also {
            val registry = MutableServiceRegistryImpl()
            it.declare<MutableServiceRegistryImpl>(
                registry,
                secondaryTypes = listOf(ServiceRegistry::class, MutableServiceRegistry::class),
                allowOverride = true,
            )
        }
    }
}
