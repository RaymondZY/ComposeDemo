package zhaoyun.example.composedemo.scaffold.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import zhaoyun.example.composedemo.scaffold.core.mvi.BaseUseCase
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEffect
import zhaoyun.example.composedemo.scaffold.core.mvi.UiEvent
import zhaoyun.example.composedemo.scaffold.core.mvi.UiState

/**
 * MVI ViewModel 基类 —— 表现层仅负责生命周期管理与平台桥接
 *
 * 所有业务逻辑已下沉到 [BaseUseCase]（位于 :domain 模块），
 * 该 ViewModel **仅**将 UI 事件广播给所有 UseCase，并暴露统一的状态流供 Compose 订阅。
 *
 * 支持一个 ViewModel 绑定多个 UseCase，它们共享同一份 [State]，并各自独立发射 [Effect]。
 */
abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect>(
    initialState: S,
    private vararg val useCases: BaseUseCase<S, E, F>
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    init {
        useCases.forEach { it.bind(_state) }
    }

    val effect = merge(*useCases.map { it.effect }.toTypedArray())
    val baseEffect = merge(*useCases.map { it.baseEffect }.toTypedArray())

    fun onEvent(event: E) {
        viewModelScope.launch {
            useCases.forEach { it.onEvent(event) }
        }
    }
}
