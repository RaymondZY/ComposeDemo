package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.LocalKoinScope
import org.koin.compose.getKoin
import org.koin.core.parameter.ParametersHolder
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import java.util.UUID

/**
 * 为业务 Screen 创建独立的 Koin Scope 和 ServiceRegistry，并在内部创建 ViewModel。
 *
 * ViewModel 会从当前 Scope 中解析 [MutableServiceRegistry]，因此无需在 Koin 全局模块中注册它。
 */
@Composable
inline fun <reified VM : BaseViewModel<*, *, *>> MviScreen(
    noinline onBaseEffect: suspend (BaseEffect) -> Boolean = { false },
    noinline parameters: (() -> ParametersHolder)? = null,
    crossinline content: @Composable (VM) -> Unit,
) {
    val koin = getKoin()
    val scopeId = remember { UUID.randomUUID().toString() }
    val koinRegistry = remember { KoinServiceRegistry(koin) }
    val screenRegistry = remember { MutableServiceRegistryImpl(parent = koinRegistry, logger = AndroidMviLogger) }
    val scope = remember {
        koin.createScope(scopeId, qualifier = MviKoinScopes.Screen).also {
            AndroidMviLogger.i("Mvi", "Scope created [MviScreenScope] id=$scopeId")
            it.declare<MutableServiceRegistryImpl>(
                screenRegistry,
                secondaryTypes = listOf(ServiceRegistry::class, MutableServiceRegistry::class),
                allowOverride = true,
            )
        }
    }
    DisposableEffect(scope) {
        onDispose {
            AndroidMviLogger.i("Mvi", "Scope closing [MviScreenScope] id=$scopeId")
            screenRegistry.clear()
            scope.close()
        }
    }
    CompositionLocalProvider(
        LocalServiceRegistry provides screenRegistry,
        LocalKoinScope provides scope,
    ) {
        val viewModel: VM = koinViewModel(scope = scope, parameters = parameters)
        LaunchedEffect(viewModel) {
            viewModel.effectDispatcher.baseEffect.collect { effect ->
                if (!onBaseEffect(effect)) {
                    throw IllegalStateException(
                        "Unhandled BaseEffect: ${effect::class.simpleName}. " +
                                "You must return 'true' in onBaseEffect for any BaseEffect " +
                                "you handle, otherwise this crash occurs."
                    )
                }
            }
        }
        content(viewModel)
    }
}
