package zhaoyun.example.composedemo.domain.model

sealed class TodoEffect {
    data class ShowToast(val message: String) : TodoEffect()
    data object ClearInput : TodoEffect()
}
