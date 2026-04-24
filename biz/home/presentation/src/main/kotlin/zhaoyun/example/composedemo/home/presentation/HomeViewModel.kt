package zhaoyun.example.composedemo.home.presentation

import zhaoyun.example.composedemo.home.domain.HomeEffect
import zhaoyun.example.composedemo.home.domain.HomeEvent
import zhaoyun.example.composedemo.home.domain.HomeState
import zhaoyun.example.composedemo.home.domain.HomeUseCase
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.StateHolder

class HomeViewModel(
    useCase: HomeUseCase,
    injectedStateHolder: StateHolder<HomeState>? = null
) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(
    HomeState(),
    injectedStateHolder,
    useCase
)
