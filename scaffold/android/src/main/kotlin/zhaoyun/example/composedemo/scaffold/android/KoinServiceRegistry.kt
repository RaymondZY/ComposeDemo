package zhaoyun.example.composedemo.scaffold.android

import org.koin.core.Koin
import org.koin.core.qualifier.named
import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistry

class KoinServiceRegistry(private val koin: Koin) : ServiceRegistry {
    override fun <T : Any> find(clazz: Class<T>, tag: String?): T? =
        koin.getOrNull(clazz.kotlin, tag?.let { named(it) })
}
