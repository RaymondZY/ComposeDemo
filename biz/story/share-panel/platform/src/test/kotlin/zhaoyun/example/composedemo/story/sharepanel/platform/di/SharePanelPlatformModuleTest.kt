package zhaoyun.example.composedemo.story.sharepanel.platform.di

import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import zhaoyun.example.composedemo.scaffold.platform.MviKoinScopes
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import zhaoyun.example.composedemo.story.sharepanel.core.SharePanelState
import zhaoyun.example.composedemo.story.sharepanel.platform.SharePanelViewModel

class SharePanelPlatformModuleTest {

    @Test
    fun `share panel view model resolves from item scope`() {
        val koin = koinApplication {
            modules(sharePanelPlatformModule)
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
