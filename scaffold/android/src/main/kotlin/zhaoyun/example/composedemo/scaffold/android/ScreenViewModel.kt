package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder
import org.koin.core.parameter.parametersOf

/**
 * [koinViewModel] 的包装，自动将 ViewModel 注册到当前 Screen 的 [LocalServiceRegistry]。
 *
 * 所有位于 Screen 作用域内的 ViewModel 都应通过此 API 获取，
 * 而非直接使用 [koinViewModel]，以确保其 UseCase 提供的服务被自动注册。
 *
 * @param parameters Koin 参数构造器
 */
@Composable
inline fun <reified VM : BaseViewModel<*, *, *>> screenViewModel(
    noinline parameters: (() -> ParametersHolder)? = null
): VM {
    val registry = checkNotNull(LocalServiceRegistry.current) {
        "screenViewModel() must be called inside a Screen that provides LocalServiceRegistry. " +
        "Wrap your Screen root with ServiceRegistryProvider { ... }"
    }

    val viewModel = if (parameters != null) {
        koinViewModel<VM>(parameters = parameters)
    } else {
        koinViewModel<VM>()
    }

    DisposableEffect(viewModel) {
        viewModel.attachToRegistry(registry)
        onDispose { viewModel.detachFromRegistry(registry) }
    }

    return viewModel
}
