package zhaoyun.example.composedemo.scaffold.platform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
 *
 * 框架已内置对 [BaseEffect.ShowSnackbar] 的处理：在 Screen 底部自动显示 Snackbar。
 * 其他 [BaseEffect] 仍需要通过 [onBaseEffect] 显式处理，否则将抛出异常。
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
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(viewModel) {
            viewModel.effectDispatcher.baseEffect.collect { effect ->
                val consumedByUser = onBaseEffect(effect)
                if (!consumedByUser) {
                    when (effect) {
                        is BaseEffect.ShowSnackbar -> {
                            snackbarHostState.showSnackbar(
                                message = effect.message,
                                actionLabel = effect.actionLabel,
                            )
                        }

                        else -> {
                            throw IllegalStateException(
                                "Unhandled BaseEffect: ${effect::class.simpleName}. " +
                                    "You must return 'true' in onBaseEffect for any BaseEffect " +
                                    "you handle, otherwise this crash occurs."
                            )
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            content(viewModel)
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
