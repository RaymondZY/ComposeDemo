package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder
import org.koin.core.parameter.parametersOf

@Composable
inline fun <reified D, reified VM : BaseViewModel<*, *, *>> screenViewModel(
    keyData: D,
    noinline parameters: (() -> ParametersHolder)? = null
): VM {
    val registry = checkNotNull(LocalServiceRegistry.current) {
        "screenViewModel() must be called inside a Screen that provides LocalServiceRegistry. " +
                "Wrap your Screen root with ServiceRegistryProvider { ... }"
    }

    val key = VM::class.simpleName + " " + D::class.simpleName + " " + keyData.hashCode()
    val parameters = parameters ?: { parametersOf(keyData) }
    val viewModel = koinViewModel<VM>(key = key, parameters = parameters)

    DisposableEffect(viewModel) {
        viewModel.attachToRegistry(registry)
        onDispose { viewModel.detachFromRegistry(registry) }
    }

    return viewModel
}
