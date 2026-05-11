package zhaoyun.example.composedemo.home.presentation

import zhaoyun.example.composedemo.home.domain.HomeEffect
import zhaoyun.example.composedemo.home.domain.HomeEvent
import zhaoyun.example.composedemo.home.domain.HomeState
import zhaoyun.example.composedemo.home.domain.HomeUseCase
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder
import zhaoyun.example.composedemo.scaffold.core.spi.MutableServiceRegistry

class HomeViewModel(
    serviceRegistry: MutableServiceRegistry,
) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(
    HomeState().toStateHolder(),
    serviceRegistry,
    { stateHolder, registry -> HomeUseCase(stateHolder, registry) }
)
