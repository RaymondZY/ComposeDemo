package zhaoyun.example.composedemo.scaffold.core.spi

/**
 * 为任何持有 [MutableServiceRegistry] 的组件提供统一的查找/注册 API。
 *
 * 实现类只需暴露 [serviceRegistry] 属性，即可通过扩展函数使用 [findService]、[registerService] 等方法。
 */
interface ServiceRegistryAccessor {
    val serviceRegistry: MutableServiceRegistry
}

inline fun <reified T : Any> ServiceRegistryAccessor.findService(tag: String? = null): T {
    return serviceRegistry.find<T>(tag)
        ?: error(
            "Service ${T::class.java.name} not found in current scope. " +
                    "Did you forget to register it from a ServiceProvider or auto-expose an MviService?"
        )
}

inline fun <reified T : Any> ServiceRegistryAccessor.findServiceOrNull(tag: String? = null): T? {
    return serviceRegistry.find<T>(tag)
}

inline fun <reified T : Any> ServiceRegistryAccessor.registerService(
    instance: T,
    tag: String? = null,
) {
    serviceRegistry.register(T::class.java, instance, tag)
}

inline fun <reified T : Any> ServiceRegistryAccessor.unregisterService(tag: String? = null) {
    serviceRegistry.unregister(T::class.java, tag)
}

fun ServiceRegistryAccessor.unregisterService(instance: Any) {
    serviceRegistry.unregister(instance)
}

fun Any.autoRegister(registry: MutableServiceRegistry) {
    val clazz = this::class.java
    val interfaces = clazz.allSuperInterfaces()
    val tag = (this as? TaggedMviService)?.serviceTag
    for (interfaceType in interfaces) {
        if (interfaceType == MviService::class.java || interfaceType == TaggedMviService::class.java) {
            continue
        }
        if (MviService::class.java.isAssignableFrom(interfaceType)) {
            @Suppress("UNCHECKED_CAST")
            val typedClazz = interfaceType as Class<Any>
            if (tag != null) {
                registry.register(typedClazz, this, tag)
            } else {
                registry.register(typedClazz, this)
            }
        }
    }
}

fun Any.autoUnregister(registry: MutableServiceRegistry) {
    registry.unregister(this)
}

private fun Class<*>.allSuperInterfaces(): Set<Class<*>> {
    val discovered = linkedSetOf<Class<*>>()
    val pending = ArrayDeque<Class<*>>()
    pending += this
    while (pending.isNotEmpty()) {
        val current = pending.removeFirst()
        current.superclass?.let { pending += it }
        current.interfaces.forEach { iface ->
            pending += iface
            discovered += iface
        }
    }
    return discovered
}
