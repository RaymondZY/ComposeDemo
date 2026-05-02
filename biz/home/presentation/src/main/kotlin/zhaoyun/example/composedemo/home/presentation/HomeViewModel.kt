package zhaoyun.example.composedemo.home.presentation

import zhaoyun.example.composedemo.home.domain.HomeEffect
import zhaoyun.example.composedemo.home.domain.HomeEvent
import zhaoyun.example.composedemo.home.domain.HomeState
import zhaoyun.example.composedemo.home.domain.HomeUseCase
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.toStateHolder

class HomeViewModel : BaseViewModel<HomeState, HomeEvent, HomeEffect>(
    HomeState().toStateHolder(),
    { stateHolder -> HomeUseCase(stateHolder) }
)
