package zhaoyun.example.composedemo.scaffold.core.spi

interface UseCaseService

interface TaggedServiceProvider {
    val serviceTag: String?
        get() = null
}
