package zhaoyun.example.composedemo.home.platform

import zhaoyun.example.composedemo.home.core.HomeEffect
import zhaoyun.example.composedemo.home.core.HomeEvent
import zhaoyun.example.composedemo.home.core.HomeState
import zhaoyun.example.composedemo.home.core.HomeUseCase
import zhaoyun.example.composedemo.scaffold.platform.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry

class HomeViewModel(
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(
    HomeState().toStateHolder(),
    serviceRegistry,
    { stateHolder, registry -> HomeUseCase(stateHolder, registry) }
)
