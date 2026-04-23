package zhaoyun.example.composedemo.service.storage.impl.di

import org.koin.dsl.module
import org.koin.android.ext.koin.androidContext
import zhaoyun.example.composedemo.service.storage.api.KeyValueStorage
import zhaoyun.example.composedemo.service.storage.impl.SharedPreferencesStorage

/**
 * Storage Koin Module —— 将 API 绑定到 SharedPreferences 实现
 */
val storageModule = module {
    single<KeyValueStorage> {
        SharedPreferencesStorage(
            context = androidContext(),
            name = "compose_demo_storage"
        )
    }
}
