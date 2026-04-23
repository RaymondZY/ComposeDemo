package zhaoyun.example.composedemo.service.storage.api

/**
 * 平台无关的 key-value 存储接口
 */
interface KeyValueStorage {

    fun getString(key: String, defaultValue: String? = null): String?

    fun putString(key: String, value: String)

    fun getInt(key: String, defaultValue: Int = 0): Int

    fun putInt(key: String, value: Int)

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    fun putBoolean(key: String, value: Boolean)

    fun remove(key: String)

    fun clear()
}
