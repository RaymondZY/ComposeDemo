package zhaoyun.example.composedemo.scaffold.core.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceProvider
import zhaoyun.example.composedemo.scaffold.core.spi.TaggedServiceProvider
import zhaoyun.example.composedemo.scaffold.core.spi.UseCaseService

class UseCaseServiceAutoRegistrationTest {

    @Test
    fun `combine use case auto registers use case services for sibling lookup`() = runTest {
        lateinit var provider: AnalyticsProviderUseCase

        val combineUseCase = CombineUseCase(
            initialState = DemoState(),
            { holder: StateHolder<DemoState> ->
                AnalyticsProviderUseCase(stateHolder = holder).also { provider = it }
            },
            { holder: StateHolder<DemoState> -> AnalyticsConsumerUseCase(stateHolder = holder) },
        )

        combineUseCase.receiveEvent(DemoEvent.Track)

        assertEquals(1, provider.trackCount)
    }

    @Test
    fun `combine use case auto registers tagged services`() = runTest {
        val combineUseCase = CombineUseCase(
            initialState = DemoState(),
            { holder: StateHolder<DemoState> -> TaggedAnalyticsProviderUseCase(stateHolder = holder) },
            { holder: StateHolder<DemoState> -> TaggedAnalyticsConsumerUseCase(stateHolder = holder) },
        )

        combineUseCase.receiveEvent(DemoEvent.Track)

        assertEquals(1, combineUseCase.state.value.trackCount)
    }

    @Test
    fun `combine use case supports manual service providers for non marker interfaces`() = runTest {
        val combineUseCase = CombineUseCase(
            initialState = DemoState(),
            { holder: StateHolder<DemoState> -> ManualProviderUseCase(stateHolder = holder) },
            { holder: StateHolder<DemoState> -> ManualConsumerUseCase(stateHolder = holder) },
        )

        combineUseCase.receiveEvent(DemoEvent.ReadManual)

        assertEquals(7, combineUseCase.state.value.trackCount)
    }

    @Test
    fun `auto registration exposes parent marker interfaces in the hierarchy`() = runTest {
        val combineUseCase = CombineUseCase(
            initialState = DemoState(),
            { holder: StateHolder<DemoState> -> HierarchicalProviderUseCase(stateHolder = holder) },
            { holder: StateHolder<DemoState> -> HierarchicalConsumerUseCase(stateHolder = holder) },
        )

        combineUseCase.receiveEvent(DemoEvent.ReadHierarchy)

        assertEquals(11, combineUseCase.state.value.trackCount)
    }

    @Test(expected = IllegalStateException::class)
    fun `duplicate auto registered services in one scope fail fast`() {
        CombineUseCase(
            initialState = DemoState(),
            { holder: StateHolder<DemoState> -> AnalyticsProviderUseCase(stateHolder = holder) },
            { holder: StateHolder<DemoState> -> DuplicateAnalyticsProviderUseCase(stateHolder = holder) },
        )
    }

    private data class DemoState(
        val trackCount: Int = 0,
    ) : UiState

    private sealed interface DemoEvent : UiEvent {
        data object Track : DemoEvent
        data object ReadManual : DemoEvent
        data object ReadHierarchy : DemoEvent
    }

    private data object DemoEffect : UiEffect

    private interface AnalyticsService : UseCaseService {
        fun track()
    }

    private interface HierarchicalAnalyticsService : UseCaseService {
        fun version(): Int
    }

    private interface DetailedAnalyticsService : HierarchicalAnalyticsService

    private interface ManualService {
        fun version(): Int
    }

    private class AnalyticsProviderUseCase(
        stateHolder: StateHolder<DemoState>? = null,
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        initialState = DemoState(),
        stateHolder = stateHolder,
    ), AnalyticsService {

        var trackCount: Int = 0
            private set

        override fun track() {
            trackCount += 1
        }

        override suspend fun onEvent(event: DemoEvent) = Unit
    }

    private class AnalyticsConsumerUseCase(
        stateHolder: StateHolder<DemoState>? = null,
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        initialState = DemoState(),
        stateHolder = stateHolder,
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            when (event) {
                DemoEvent.Track -> findService<AnalyticsService>().track()
                else -> Unit
            }
        }
    }

    private class DuplicateAnalyticsProviderUseCase(
        stateHolder: StateHolder<DemoState>? = null,
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        initialState = DemoState(),
        stateHolder = stateHolder,
    ), AnalyticsService {
        override fun track() = Unit

        override suspend fun onEvent(event: DemoEvent) = Unit
    }

    private class TaggedAnalyticsProviderUseCase(
        stateHolder: StateHolder<DemoState>? = null,
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        initialState = DemoState(),
        stateHolder = stateHolder,
    ), AnalyticsService, TaggedServiceProvider {

        override val serviceTag: String = "story"

        override fun track() = Unit

        override suspend fun onEvent(event: DemoEvent) = Unit
    }

    private class TaggedAnalyticsConsumerUseCase(
        stateHolder: StateHolder<DemoState>? = null,
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        initialState = DemoState(),
        stateHolder = stateHolder,
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            when (event) {
                DemoEvent.Track -> {
                    findService<AnalyticsService>(tag = "story").track()
                    updateState { it.copy(trackCount = it.trackCount + 1) }
                }
                else -> Unit
            }
        }
    }

    private class ManualProviderUseCase(
        stateHolder: StateHolder<DemoState>? = null,
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        initialState = DemoState(),
        stateHolder = stateHolder,
    ), ServiceProvider {
        override fun provideServices(registry: MutableServiceRegistry) {
            registry.register(ManualService::class.java, object : ManualService {
                override fun version(): Int = 7
            }, tag = "manual")
        }

        override suspend fun onEvent(event: DemoEvent) = Unit
    }

    private class ManualConsumerUseCase(
        stateHolder: StateHolder<DemoState>? = null,
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        initialState = DemoState(),
        stateHolder = stateHolder,
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            when (event) {
                DemoEvent.ReadManual -> {
                    val service = findService<ManualService>(tag = "manual")
                    updateState { it.copy(trackCount = service.version()) }
                }
                else -> Unit
            }
        }
    }

    private class HierarchicalProviderUseCase(
        stateHolder: StateHolder<DemoState>? = null,
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        initialState = DemoState(),
        stateHolder = stateHolder,
    ), DetailedAnalyticsService {
        override fun version(): Int = 11

        override suspend fun onEvent(event: DemoEvent) = Unit
    }

    private class HierarchicalConsumerUseCase(
        stateHolder: StateHolder<DemoState>? = null,
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        initialState = DemoState(),
        stateHolder = stateHolder,
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            when (event) {
                DemoEvent.ReadHierarchy -> {
                    val service = findService<HierarchicalAnalyticsService>()
                    updateState { it.copy(trackCount = service.version()) }
                }
                else -> Unit
            }
        }
    }
}
