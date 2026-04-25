package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import zhaoyun.example.composedemo.scaffold.core.mvi.MutableServiceRegistry

/**
 * CompositionLocal —— 向下传递当前 Screen 的 [MutableServiceRegistry]
 */
val LocalServiceRegistry = compositionLocalOf<MutableServiceRegistry?> { null }

/**
 * Screen 级别的 Registry Provider。
 *
 * 推荐所有 Screen 根 Composable 包裹此组件，以建立服务作用域。
 * 子 Screen 若也包裹 [ServiceRegistryProvider]，会自动继承父 Screen 的 registry 作为 parent。
 *
 * @param parentRegistry 显式指定 parent registry。若未指定，自动从 [LocalServiceRegistry] 获取。
 * @param content Screen 内容
 */
@Composable
fun ServiceRegistryProvider(
    parentRegistry: MutableServiceRegistry? = LocalServiceRegistry.current,
    content: @Composable () -> Unit
) {
    val registry = remember { MutableServiceRegistryImpl(parent = parentRegistry) }

    CompositionLocalProvider(LocalServiceRegistry provides registry) {
        content()
    }

    DisposableEffect(Unit) {
        onDispose { registry.clear() }
    }
}
