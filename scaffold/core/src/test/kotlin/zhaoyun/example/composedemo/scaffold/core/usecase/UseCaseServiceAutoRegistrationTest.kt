package zhaoyun.example.composedemo.scaffold.core.usecase

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MviService
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.ScreenScopeStack
import zhaoyun.example.composedemo.scaffold.core.spi.TaggedMviService

class UseCaseServiceAutoRegistrationTest {

    @Before
    fun setup() {
        startKoin {
            modules(module {
                scope(named("MviScreenScope")) {
                    scoped<zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry> { MutableServiceRegistryImpl() }
                }
            })
        }
    }

    @After
    fun teardown() {
        stopKoin()
        while (ScreenScopeStack.current != null) {
            ScreenScopeStack.pop()
        }
    }

    @Test
    fun `combine use case auto registers use case services for sibling lookup`() = runTest {
        val koin = org.koin.core.context.GlobalContext.get()
        val scope = koin.createScope("test", named("MviScreenScope"))
        ScreenScopeStack.push(scope)

        lateinit var provider: AnalyticsProvider

        val combineUseCase = CombineUseCase(
            DemoState().toStateHolder(),
            { holder: StateHolder<DemoState> ->
                AnalyticsProvider(stateHolder = holder).also { provider = it }
            },
            { holder: StateHolder<DemoState> -> AnalyticsConsumerUseCase(stateHolder = holder) },
        )

        combineUseCase.receiveEvent(DemoEvent.Track)

        assertEquals(1, provider.trackCount)

        ScreenScopeStack.pop()
        scope.close()
    }

    @Test
    fun `combine use case auto registers tagged services`() = runTest {
        val koin = org.koin.core.context.GlobalContext.get()
        val scope = koin.createScope("test2", named("MviScreenScope"))
        ScreenScopeStack.push(scope)

        val combineUseCase = CombineUseCase(
            DemoState().toStateHolder(),
            { holder: StateHolder<DemoState> -> TaggedAnalytics(stateHolder = holder) },
            { holder: StateHolder<DemoState> -> TaggedAnalyticsConsumerUseCase(stateHolder = holder) },
        )

        combineUseCase.receiveEvent(DemoEvent.Track)

        assertEquals(1, combineUseCase.state.value.trackCount)

        ScreenScopeStack.pop()
        scope.close()
    }

    @Test
    fun `auto registration exposes parent marker interfaces in the hierarchy`() = runTest {
        val koin = org.koin.core.context.GlobalContext.get()
        val scope = koin.createScope("test3", named("MviScreenScope"))
        ScreenScopeStack.push(scope)

        val combineUseCase = CombineUseCase(
            DemoState().toStateHolder(),
            { holder: StateHolder<DemoState> -> HierarchicalProvider(stateHolder = holder) },
            { holder: StateHolder<DemoState> -> HierarchicalConsumerUseCase(stateHolder = holder) },
        )

        combineUseCase.receiveEvent(DemoEvent.ReadHierarchy)

        assertEquals(11, combineUseCase.state.value.trackCount)

        ScreenScopeStack.pop()
        scope.close()
    }

    @Test(expected = IllegalStateException::class)
    fun `duplicate auto registered services in one scope fail fast`() {
        val koin = org.koin.core.context.GlobalContext.get()
        val scope = koin.createScope("test4", named("MviScreenScope"))
        ScreenScopeStack.push(scope)

        try {
            CombineUseCase(
                DemoState().toStateHolder(),
                { holder: StateHolder<DemoState> -> AnalyticsProvider(stateHolder = holder) },
                { holder: StateHolder<DemoState> -> DuplicateAnalyticsProvider(stateHolder = holder) },
            )
        } finally {
            ScreenScopeStack.pop()
            scope.close()
        }
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
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
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
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
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
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
    ), AnalyticsMviService {
        override fun track() = Unit

        override suspend fun onEvent(event: DemoEvent) = Unit
    }

    private class TaggedAnalytics(
        stateHolder: StateHolder<DemoState>? = null,
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
    ), AnalyticsMviService, TaggedMviService {

        override val serviceTag: String = "story"

        override fun track() = Unit

        override suspend fun onEvent(event: DemoEvent) = Unit
    }

    private class TaggedAnalyticsConsumerUseCase(
        stateHolder: StateHolder<DemoState>? = null,
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
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
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
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
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
    ), DetailedAnalyticsMviService {
        override fun version(): Int = 11

        override suspend fun onEvent(event: DemoEvent) = Unit
    }

    private class HierarchicalConsumerUseCase(
        stateHolder: StateHolder<DemoState>? = null,
    ) : BaseUseCase<DemoState, DemoEvent, DemoEffect>(
        stateHolder = stateHolder ?: DemoState().toStateHolder(),
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
