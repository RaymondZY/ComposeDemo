package zhaoyun.example.composedemo.scaffold.core.usecase

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.MviService
import zhaoyun.example.composedemo.scaffold.core.spi.TaggedMviService
import zhaoyun.example.composedemo.scaffold.core.spi.findService

class UseCaseServiceAutoRegistrationTest {

    @Test
    fun `combine use case auto registers use case services for sibling lookup`() = runTest {
        val registry = MutableServiceRegistryImpl()
        lateinit var provider: AnalyticsProvider

        val combineUseCase = CombineUseCase(
            DemoState().toStateHolder(),
            registry,
            { holder: StateHolder<DemoState>, reg ->
                AnalyticsProvider(stateHolder = holder, serviceRegistry = reg).also { provider = it }
            },
            { holder: StateHolder<DemoState>, reg -> AnalyticsConsumerUseCase(stateHolder = holder, serviceRegistry = reg) },
        )

        combineUseCase.receiveEvent(DemoEvent.Track)

        assertEquals(1, provider.trackCount)
        combineUseCase.onCleared()
    }

    @Test
    fun `combine use case auto registers tagged services`() = runTest {
        val registry = MutableServiceRegistryImpl()
        val combineUseCase = CombineUseCase(
            DemoState().toStateHolder(),
            registry,
            { holder: StateHolder<DemoState>, reg -> TaggedAnalytics(stateHolder = holder, serviceRegistry = reg) },
            { holder: StateHolder<DemoState>, reg -> TaggedAnalyticsConsumerUseCase(stateHolder = holder, serviceRegistry = reg) },
        )

        combineUseCase.receiveEvent(DemoEvent.Track)

        assertEquals(1, combineUseCase.state.value.trackCount)
        combineUseCase.onCleared()
    }

    @Test
    fun `auto registration exposes parent marker interfaces in the hierarchy`() = runTest {
        val registry = MutableServiceRegistryImpl()
        val combineUseCase = CombineUseCase(
            DemoState().toStateHolder(),
            registry,
            { holder: StateHolder<DemoState>, reg -> HierarchicalProvider(stateHolder = holder, serviceRegistry = reg) },
            { holder: StateHolder<DemoState>, reg -> HierarchicalConsumerUseCase(stateHolder = holder, serviceRegistry = reg) },
        )

        combineUseCase.receiveEvent(DemoEvent.ReadHierarchy)

        assertEquals(11, combineUseCase.state.value.trackCount)
        combineUseCase.onCleared()
    }

    @Test(expected = IllegalStateException::class)
    fun `duplicate auto registered services in one scope fail fast`() {
        val registry = MutableServiceRegistryImpl()
        CombineUseCase(
            DemoState().toStateHolder(),
            registry,
            { holder: StateHolder<DemoState>, reg -> AnalyticsProvider(stateHolder = holder, serviceRegistry = reg) },
            { holder: StateHolder<DemoState>, reg -> DuplicateAnalyticsProvider(stateHolder = holder, serviceRegistry = reg) },
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

    private interface AnalyticsMviService : MviService {
        fun track()
    }

    private interface HierarchicalAnalyticsMviService : MviService {
        fun version(): Int
    }

    private interface DetailedAnalyticsMviService : HierarchicalAnalyticsMviService

    private interface ManualService {
        fun version(): Int
    }

    private class AnalyticsProvider(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
        serviceRegistry = serviceRegistry,
    ), AnalyticsMviService {

        var trackCount: Int = 0
            private set

        override fun track() {
            trackCount += 1
        }

        override suspend fun onEvent(event: DemoEvent) = Unit
    }

    private class AnalyticsConsumerUseCase(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
        serviceRegistry = serviceRegistry,
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            when (event) {
                DemoEvent.Track -> findService<AnalyticsMviService>().track()
                else -> Unit
            }
        }
    }

    private class DuplicateAnalyticsProvider(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
        serviceRegistry = serviceRegistry,
    ), AnalyticsMviService {
        override fun track() = Unit

        override suspend fun onEvent(event: DemoEvent) = Unit
    }

    private class TaggedAnalytics(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
        serviceRegistry = serviceRegistry,
    ), AnalyticsMviService, TaggedMviService {

        override val serviceTag: String = "story"

        override fun track() = Unit

        override suspend fun onEvent(event: DemoEvent) = Unit
    }

    private class TaggedAnalyticsConsumerUseCase(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
        serviceRegistry = serviceRegistry,
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            when (event) {
                DemoEvent.Track -> {
                    findService<AnalyticsMviService>(tag = "story").track()
                    updateState { it.copy(trackCount = it.trackCount + 1) }
                }
                else -> Unit
            }
        }
    }

    private class ManualConsumerUseCase(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
        serviceRegistry = serviceRegistry,
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

    private class HierarchicalProvider(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
        serviceRegistry = serviceRegistry,
    ), DetailedAnalyticsMviService {
        override fun version(): Int = 11

        override suspend fun onEvent(event: DemoEvent) = Unit
    }

    private class HierarchicalConsumerUseCase(
        stateHolder: StateHolder<DemoState>? = null,
        serviceRegistry: MutableServiceRegistry = MutableServiceRegistryImpl(),
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
        serviceRegistry = serviceRegistry,
    ) {
        override suspend fun onEvent(event: DemoEvent) {
            when (event) {
                DemoEvent.ReadHierarchy -> {
                    val service = findService<HierarchicalAnalyticsMviService>()
                    updateState { it.copy(trackCount = service.version()) }
                }
                else -> Unit
            }
        }
    }
}
