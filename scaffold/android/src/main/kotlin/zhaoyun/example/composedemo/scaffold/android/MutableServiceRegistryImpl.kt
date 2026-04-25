package zhaoyun.example.composedemo.scaffold.android

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.error.NoBeanDefFoundException
import zhaoyun.example.composedemo.scaffold.core.mvi.MutableServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.mvi.ServiceRegistry
import zhaoyun.example.composedemo.scaffold.core.mvi.ServiceProvider

/**
 * [MutableServiceRegistry] 的默认实现。
 *
 * 查找顺序：本地注册 → Parent registry → Koin 全局容器
 */
class MutableServiceRegistryImpl(
    private val parent: ServiceRegistry? = null
) : MutableServiceRegistry, KoinComponent {

    private val services = mutableMapOf<Class<*>, Any>()

    override fun <T : Any> register(clazz: Class<T>, instance: T) {
        services[clazz] = instance
    }

    override fun unregister(instance: Any) {
        services.values.remove(instance)
    }

    override fun clear() {
        services.clear()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> find(clazz: Class<T>): T? {
        return services[clazz] as? T           // 1. 本地（同 Screen 内注册）
            ?: parent?.find(clazz)             // 2. Parent 链（父/祖父 Screen）
            ?: try { getKoin().get(clazz.kotlin) }  // 3. Koin 全局容器 fallback
               catch (_: NoBeanDefFoundException) { null }
    }
}
