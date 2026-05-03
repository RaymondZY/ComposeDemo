package zhaoyun.example.composedemo.scaffold.core.context

import zhaoyun.example.composedemo.scaffold.core.spi.ServiceRegistryAccessor

interface MviContext : ServiceRegistryAccessor {
    fun logger(): MviLogger = serviceRegistry.find(MviLogger::class.java) ?: NoOpMviLogger
}
