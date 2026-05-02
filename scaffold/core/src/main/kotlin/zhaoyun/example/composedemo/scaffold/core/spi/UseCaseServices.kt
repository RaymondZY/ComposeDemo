package zhaoyun.example.composedemo.scaffold.core.spi

interface MviService

interface TaggedMviService : MviService {
    val serviceTag: String
}
