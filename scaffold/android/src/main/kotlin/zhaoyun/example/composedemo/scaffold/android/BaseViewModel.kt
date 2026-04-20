package zhaoyun.example.composedemo.scaffold.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

/**
 * MVI ViewModel 基类 —— 表现层仅负责生命周期管理与平台桥接
 *
 * 所有业务逻辑已下沉到 [BaseUseCase]（位于 :domain 模块），
 * 该 ViewModel **仅**将 UI 事件转发给 UseCase，并暴露状态流供 Compose 订阅。
 */
abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    private val useCase: BaseUseCase<S, E, F>
) : ViewModel() {

    val state = useCase.state
    val effect = useCase.effect
    val baseEffect = useCase.baseEffect

    fun onEvent(event: E) {
        viewModelScope.launch {
            useCase.onEvent(event)
        }
    }
}
