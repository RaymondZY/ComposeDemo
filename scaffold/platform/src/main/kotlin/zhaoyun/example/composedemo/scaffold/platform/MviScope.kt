package zhaoyun.example.composedemo.scaffold.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import org.koin.compose.LocalKoinScope
import org.koin.compose.getKoin
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistryImpl
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry
import java.util.UUID

@Composable
fun MviScope(
    scopeId: String = remember { UUID.randomUUID().toString() },
    parentRegistry: ServiceRegistry? = null,
    content: @Composable () -> Unit,
) {
    val koin = getKoin()
    val registry = remember(parentRegistry) {
        MutableServiceRegistryImpl(parent = parentRegistry, logger = AndroidMviLogger)
    }
    val scope = remember(scopeId) {
        koin.createScope(scopeId, qualifier = MviKoinScopes.Item).also {
            AndroidMviLogger.i("Mvi", "Scope created [MviScope] id=$scopeId")
            it.declare<MutableServiceRegistryImpl>(
                registry,
                secondaryTypes = listOf(ServiceRegistry::class, MutableServiceRegistry::class),
                allowOverride = true,
            )
        }
    }
    DisposableEffect(scope) {
        onDispose {
            AndroidMviLogger.i("Mvi", "Scope closing [MviScope] id=$scopeId")
            registry.clear()
            scope.close()
        }
    }
    CompositionLocalProvider(
        LocalServiceRegistry provides registry,
        LocalKoinScope provides scope,
    ) {
        content()
    }
}

@Composable
fun MviItemScope(
    scopeId: String = remember { UUID.randomUUID().toString() },
    content: @Composable () -> Unit,
) {
    val parentRegistry = LocalServiceRegistry.current
    MviScope(scopeId = scopeId, parentRegistry = parentRegistry, content = content)
}
