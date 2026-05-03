package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.runtime.Composable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.LocalKoinScope
import org.koin.core.parameter.parametersOf

@Composable
inline fun <reified D, reified VM : BaseViewModel<*, *, *>> screenViewModel(
    keyData: D,
    noinline parameters: (() -> org.koin.core.parameter.ParametersHolder)? = null,
): VM {
    val scope = checkNotNull(LocalKoinScope.current)
    val key = VM::class.simpleName + " " + D::class.simpleName + " " + keyData.hashCode()
    val params = parameters ?: { parametersOf(keyData) }
    return koinViewModel<VM>(key = key, scope = scope, parameters = params)
}
