package zhaoyun.example.composedemo.feed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import zhaoyun.example.composedemo.home.presentation.HomeScreen
import zhaoyun.example.composedemo.ui.theme.TodoListTheme

class FeedActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TodoListTheme {
                HomeScreen()
            }
        }
    }
}
