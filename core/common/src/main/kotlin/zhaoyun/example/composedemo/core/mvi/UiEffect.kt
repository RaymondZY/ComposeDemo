package zhaoyun.example.composedemo.core.mvi

/**
 * 通用 UI 副作用 —— 跨业务模块复用
 */
sealed class UiEffect {
    data class ShowToast(val message: String) : UiEffect()
    data object NavigateToHome : UiEffect()
    data object ClearInput : UiEffect()
}
