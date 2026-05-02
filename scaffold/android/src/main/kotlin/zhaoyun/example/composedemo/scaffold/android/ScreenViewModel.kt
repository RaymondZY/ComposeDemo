package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.runtime.Composable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.ParametersHolder
import org.koin.core.parameter.parametersOf
import zhaoyun.example.composedemo.scaffold.core.spi.ScreenScopeStack

@Composable
inline fun <reified D, reified VM : BaseViewModel<*, *, *>> screenViewModel(
    keyData: D,
    noinline parameters: (() -> ParametersHolder)? = null
): VM {
    val scope = checkNotNull(LocalKoinScope.current) {
        "screenViewModel() must be called inside a Screen that provides LocalKoinScope. " +
            "Wrap your Screen root with MviScreen { ... }"
    }

    val key = VM::class.simpleName + " " + D::class.simpleName + " " + keyData.hashCode()
    val params = parameters ?: { parametersOf(keyData) }

    ScreenScopeStack.push(scope)
    val viewModel = koinViewModel<VM>(
        key = key,
        scope = scope,
        parameters = params
    )
    ScreenScopeStack.pop()
    return viewModel
}
