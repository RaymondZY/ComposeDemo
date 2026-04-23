package zhaoyun.example.composedemo.home.domain

import org.koin.dsl.module

val homeDomainModule = module {
    factory { HomeUseCase() }
}
