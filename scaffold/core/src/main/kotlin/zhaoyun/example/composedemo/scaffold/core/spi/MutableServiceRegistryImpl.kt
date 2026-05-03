package zhaoyun.example.composedemo.scaffold.core.spi

import zhaoyun.example.composedemo.scaffold.core.context.MviLogger
import zhaoyun.example.composedemo.scaffold.core.context.NoOpMviLogger

class MutableServiceRegistryImpl(
    private val parent: ServiceRegistry? = null,
    logger: MviLogger? = null,
) : MutableServiceRegistry {

    private val logger = logger ?: NoOpMviLogger
    private val services = linkedMapOf<ServiceKey<*>, Any>()

    init {
        services[ServiceKey(MviLogger::class.java, null)] = this.logger
    }

    override fun <T : Any> register(clazz: Class<T>, instance: T, tag: String?) {
        val key = ServiceKey(clazz, tag)
        check(key !in services) {
            "Duplicate service registration for ${clazz.name} with tag=$tag in the same scope"
        }
        services[key] = instance
        logger.i("Mvi", "Register ${clazz.name}${tag?.let { " tag=$it" } ?: ""}")
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> find(clazz: Class<T>, tag: String?): T? {
        val key = ServiceKey(clazz, tag)
        val result = services[key] as? T ?: parent?.find(clazz, tag)
        if (result == null) {
            logger.d("Mvi", "Miss   ${clazz.name}${tag?.let { " tag=$it" } ?: ""}")
        }
        return result
    }

    override fun unregister(clazz: Class<*>, tag: String?) {
        services.remove(ServiceKey(clazz, tag))
        logger.i("Mvi", "Unregister ${clazz.name}${tag?.let { " tag=$it" } ?: ""}")
    }

    override fun unregister(instance: Any) {
        val removed = services.entries.removeAll { it.value === instance }
        if (removed) {
            logger.i("Mvi", "Unregister instance ${instance::class.java.name}")
        }
    }

    override fun clear() {
        logger.i("Mvi", "Clear ${services.size} services")
        services.clear()
    }
}
