package zhaoyun.example.composedemo.scaffold.core.mvi

interface BaseEffect : UiEffect {
    data class ShowToast(val message: String) : BaseEffect
}
