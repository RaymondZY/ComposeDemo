package zhaoyun.example.composedemo.scaffold.core.mvi

/**
 * 通用基础副作用 —— 由 [MviScreen] 统一处理或分发
 *
 * 业务模块的 Effect 可通过实现此接口混入通用行为，
 * 也可直接发送 [BaseEffect] 的具体子类。
 */
sealed interface BaseEffect : UiEffect {
    data class ShowToast(val message: String) : BaseEffect
    data class ShowDialog(val title: String, val message: String) : BaseEffect
    data object NavigateBack : BaseEffect
}
