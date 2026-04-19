package zhaoyun.example.composedemo.domain.model

data class TodoItem(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val isCompleted: Boolean = false
)
