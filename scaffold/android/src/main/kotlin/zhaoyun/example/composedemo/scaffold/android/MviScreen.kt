package zhaoyun.example.composedemo.scaffold.android

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import org.koin.compose.getKoin
import org.koin.core.qualifier.named
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.ScreenScopeStack

@Composable
fun <S : UiState, E : UiEvent, F : UiEffect> MviScreen(
    viewModel: BaseViewModel<S, E, F>,
    onBaseEffect: suspend (BaseEffect) -> Boolean = { false },
    content: @Composable () -> Unit
) {
    val koin = getKoin()
    val screenRegistry = remember { MutableServiceRegistryImpl() }
    val scope = remember(viewModel) {
        val scopeId = "MviScreen_${viewModel.hashCode()}_${System.currentTimeMillis()}"
        koin.createScope(scopeId, qualifier = named("MviScreenScope"))
    }

    DisposableEffect(scope) {
        scope.declare(screenRegistry, allowOverride = true)
        ScreenScopeStack.push(scope)
        viewModel.ensureRegistered(screenRegistry)
        onDispose {
            screenRegistry.clear()
            scope.close()
            ScreenScopeStack.pop()
        }
    }

    ServiceRegistryProvider(registry = screenRegistry) {
        CompositionLocalProvider(LocalKoinScope provides scope) {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.baseEffect.collect { effect ->
                    val consumed = onBaseEffect(effect)
                    if (!consumed) {
                        defaultHandleBaseEffect(context, effect)
                    }
                }
            }
            content()
        }
    }
}

suspend fun defaultHandleBaseEffect(context: Context, effect: BaseEffect) {
    when (effect) {
        is BaseEffect.ShowToast -> {
            Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
        }
        else -> {
            // other BaseEffect default not handled
        }
    }
}
