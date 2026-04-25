package zhaoyun.example.composedemo.scaffold.core.mvi

/**
 * 只读服务发现接口 —— UseCase 通过此接口查找服务
 */
interface ServiceRegistry {
    fun <T : Any> find(clazz: Class<T>): T?
}

inline fun <reified T : Any> ServiceRegistry.find(): T? = find(T::class.java)

/**
 * 可变服务注册表 —— 用于注册和注销服务
 */
interface MutableServiceRegistry : ServiceRegistry {
    fun <T : Any> register(clazz: Class<T>, instance: T)
    fun unregister(instance: Any)
    fun clear()
}

inline fun <reified T : Any> MutableServiceRegistry.register(instance: T) = register(T::class.java, instance)

/**
 * 服务提供者契约 —— UseCase 实现此接口以声明"我提供什么服务"
 */
interface ServiceProvider {
    fun provideServices(registry: MutableServiceRegistry)
}
