package zhaoyun.example.composedemo.scaffold.core.spi

class MutableServiceRegistryImpl(
    private var parent: ServiceRegistry? = null,
) : MutableServiceRegistry {

    private val services = linkedMapOf<ServiceKey<*>, Any>()

    override fun <T : Any> register(clazz: Class<T>, instance: T, tag: String?) {
        val key = ServiceKey(clazz, tag)
        check(key !in services) {
            "Duplicate service registration for ${clazz.name} with tag=$tag in the same scope"
        }
        services[key] = instance
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> find(clazz: Class<T>, tag: String?): T? {
        val key = ServiceKey(clazz, tag)
        return services[key] as? T ?: parent?.find(clazz, tag)
    }

    override fun unregister(clazz: Class<*>, tag: String?) {
        services.remove(ServiceKey(clazz, tag))
    }

    override fun unregister(instance: Any) {
        services.entries.removeAll { it.value === instance }
    }

    override fun clear() {
        services.clear()
    }

    override fun attachParent(serviceRegistry: ServiceRegistry) {
        parent = serviceRegistry
    }

    override fun detachParent() {
        parent = null
    }
}
