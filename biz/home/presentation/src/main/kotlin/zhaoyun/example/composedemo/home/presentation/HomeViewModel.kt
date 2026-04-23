package zhaoyun.example.composedemo.home.presentation

import zhaoyun.example.composedemo.home.domain.HomeEffect
import zhaoyun.example.composedemo.home.domain.HomeEvent
import zhaoyun.example.composedemo.home.domain.HomeState
import zhaoyun.example.composedemo.home.domain.HomeUseCase
import zhaoyun.example.composedemo.scaffold.android.BaseViewModel
import zhaoyun.example.composedemo.scaffold.core.mvi.Reducer

class HomeViewModel(
    useCase: HomeUseCase,
    private val injectedReducer: Reducer<HomeState>? = null
) : BaseViewModel<HomeState, HomeEvent, HomeEffect>(
    initialState = HomeState(),
    useCase
) {
    override fun createReducer(initialState: HomeState): Reducer<HomeState> {
        return injectedReducer ?: super.createReducer(initialState)
    }
}
