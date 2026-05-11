package zhaoyun.example.composedemo.scaffold.android

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

@Composable
inline fun <reified T : Any> RegisterService(
    instance: T,
    tag: String? = null,
) {
    val registry = checkNotNull(LocalServiceRegistry.current) {
        "RegisterService() must be used inside a Screen that provides LocalServiceRegistry."
    }

    DisposableEffect(registry, instance, tag) {
        registry.register(T::class.java, instance, tag)
        onDispose {
            registry.unregister(instance)
        }
    }
}
