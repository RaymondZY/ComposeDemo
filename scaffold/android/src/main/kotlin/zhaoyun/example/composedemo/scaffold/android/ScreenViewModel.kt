package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.runtime.Composable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.LocalKoinScope

@Composable
inline fun <reified VM : BaseViewModel<*, *, *>> screenViewModel(
    key: String? = null,
    noinline parameters: (() -> org.koin.core.parameter.ParametersHolder)? = null,
): VM {
    val scope = checkNotNull(LocalKoinScope.current)
    val vmKey = key?.let { "${VM::class.simpleName}:$it" } ?: checkNotNull(VM::class.simpleName)
    return koinViewModel<VM>(key = vmKey, scope = scope, parameters = parameters)
}
