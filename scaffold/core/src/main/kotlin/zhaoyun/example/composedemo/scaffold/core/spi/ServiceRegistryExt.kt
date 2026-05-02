package zhaoyun.example.composedemo.scaffold.core.spi

import org.koin.core.scope.Scope

fun requireServiceRegistry(): MutableServiceRegistry {
    return ScreenScopeStack.requireCurrent().get()
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
