package zhaoyun.example.composedemo.story.sharepanel.presentation.di

import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import zhaoyun.example.composedemo.scaffold.android.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.story.sharepanel.domain.SharePanelState
import zhaoyun.example.composedemo.story.sharepanel.presentation.SharePanelViewModel

class SharePanelPresentationModuleTest {

    @Test
    fun `share panel view model resolves from item scope`() {
        val koin = koinApplication {
            modules(sharePanelPresentationModule)
        }.koin
        val scope = koin.createScope("share-panel", MviKoinScopes.Item).also {
            val registry = MutableServiceRegistryImpl()
            it.declare<MutableServiceRegistryImpl>(
                registry,
                secondaryTypes = listOf(ServiceRegistry::class, MutableServiceRegistry::class),
                allowOverride = true,
            )
        }

        val viewModel = scope.get<SharePanelViewModel> {
            parametersOf(
                "story-1",
                "https://example.com/bg.jpg",
                SharePanelState(
                    cardId = "story-1",
                    backgroundImageUrl = "https://example.com/bg.jpg",
                ).toStateHolder(),
            )
        }

        assertEquals("story-1", viewModel.state.value.cardId)
        assertEquals("https://example.com/bg.jpg", viewModel.state.value.backgroundImageUrl)
    }
}
