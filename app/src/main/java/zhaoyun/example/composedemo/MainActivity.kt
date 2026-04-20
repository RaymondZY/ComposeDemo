package zhaoyun.example.composedemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.koin.androidx.compose.getKoin
import zhaoyun.example.composedemo.login.presentation.LoginScreen
import zhaoyun.example.composedemo.service.usercenter.api.UserRepository
import zhaoyun.example.composedemo.todo.presentation.TodoListScreen

/**
 * 应用唯一入口 —— 单 Activity + Navigation Compose
 *
 * 所有页面路由在此声明，业务模块通过 Composable 与回调接口解耦。
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val userRepository: UserRepository = getKoin().get()
    val startDestination = if (userRepository.isLoggedIn()) "todo" else "login"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("todo") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("todo") {
            TodoListScreen(
                onNavigateToLogin = {
                    navController.navigate("login")
                }
            )
        }
    }
}
