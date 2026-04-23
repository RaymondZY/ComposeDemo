package zhaoyun.example.composedemo.service.storage.mock

import zhaoyun.example.composedemo.service.storage.api.KeyValueStorage

/**
 * 内存假实现 —— 供各层单元测试共用
 */
class FakeKeyValueStorage : KeyValueStorage {

    private val store = mutableMapOf<String, Any?>()

    override fun getString(key: String, defaultValue: String?): String? {
        return store[key] as? String ?: defaultValue
    }

    override fun putString(key: String, value: String) {
        store[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return store[key] as? Int ?: defaultValue
    }

    override fun putInt(key: String, value: Int) {
        store[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return store[key] as? Boolean ?: defaultValue
    }

    override fun putBoolean(key: String, value: Boolean) {
        store[key] = value
    }

    override fun remove(key: String) {
        store.remove(key)
    }

    override fun clear() {
        store.clear()
    }

    /**
     * 测试辅助：是否包含指定 key
     */
    fun contains(key: String): Boolean = store.containsKey(key)
}
