package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.runtime.Composable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder
import org.koin.core.parameter.parametersOf
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry

@Composable
inline fun <reified D, reified VM : BaseViewModel<*, *, *>> screenViewModel(
    keyData: D,
    noinline parameters: ((ServiceRegistry) -> ParametersHolder)? = null
): VM {
    val parentRegistry = checkNotNull(LocalServiceRegistry.current) {
        "screenViewModel() must be called inside a Screen that provides LocalServiceRegistry. " +
            "Wrap your Screen root with MviScreen { ... } or ServiceRegistryProvider(registry = ...)."
    }

    val key = VM::class.simpleName + " " + D::class.simpleName + " " + keyData.hashCode()
    val parameterFactory = parameters ?: { registry -> parametersOf(keyData, registry) }
    return koinViewModel(key = key, parameters = { parameterFactory(parentRegistry) })
}
