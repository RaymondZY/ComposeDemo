package zhaoyun.example.composedemo.scaffold.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import org.koin.core.scope.Scope
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry

/**
 * CompositionLocal —— 向下传递当前 Screen 的 [MutableServiceRegistry]
 */
val LocalServiceRegistry = compositionLocalOf<MutableServiceRegistry?> { null }

/**
 * CompositionLocal —— 向下传递当前 Screen 的 Koin [Scope]
 */
val LocalKoinScope = compositionLocalOf<Scope?> { null }

/**
 * 向当前 Compose 子树暴露既有的服务作用域。
 */
@Composable
fun ServiceRegistryProvider(
    registry: MutableServiceRegistry,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalServiceRegistry provides registry) {
        content()
    }
}
