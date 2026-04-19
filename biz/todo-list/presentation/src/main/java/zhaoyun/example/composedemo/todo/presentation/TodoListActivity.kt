package zhaoyun.example.composedemo.todo.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

/**
 * Todo List 页面入口 —— 位于 :biz:todo-list:presentation 模块
 *
 * 仅作为容器负责 setContent，所有登录状态检查与 UI 决策
 * 均下沉到 [TodoViewModel] 与 Compose 层处理。
 */
class TodoListActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface {
                    TodoListScreen()
                }
            }
        }
    }
}
