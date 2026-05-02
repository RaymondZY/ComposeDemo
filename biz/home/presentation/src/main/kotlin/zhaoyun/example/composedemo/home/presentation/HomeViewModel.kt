package zhaoyun.example.composedemo.home.presentation

import zhaoyun.example.composedemo.home.domain.HomeEffect
import zhaoyun.example.composedemo.home.domain.HomeEvent
import zhaoyun.example.composedemo.home.domain.HomeState
import zhaoyun.example.composedemo.home.domain.HomeUseCase
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel

class HomeViewModel : BaseViewModel<HomeState, HomeEvent, HomeEffect>(
    HomeState(),
    { stateHolder -> HomeUseCase(stateHolder) }
)
