package zhaoyun.example.composedemo.scaffold.core.spi

data class ServiceKey<T : Any>(
    val clazz: Class<T>,
    val tag: String? = null,
)

interface ServiceRegistry {
    fun <T : Any> find(clazz: Class<T>, tag: String? = null): T?
}

inline fun <reified T : Any> ServiceRegistry.find(tag: String? = null): T? =
    find(T::class.java, tag)

interface MutableServiceRegistry : ServiceRegistry {
    fun attachParent(serviceRegistry: ServiceRegistry)
    fun detachParent()
    fun <T : Any> register(clazz: Class<T>, instance: T, tag: String? = null)
    fun unregister(clazz: Class<*>, tag: String? = null)
    fun unregister(instance: Any)
    fun clear()
}

inline fun <reified T : Any> MutableServiceRegistry.register(
    instance: T,
    tag: String? = null,
) = register(T::class.java, instance, tag)
