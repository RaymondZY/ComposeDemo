package zhaoyun.example.composedemo.login.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface

/**
 * 登录页面入口 —— 位于 :biz:login:presentation 模块
 */
class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface {
                    LoginScreen(
                        onLoginSuccess = {
                            setResult(RESULT_OK)
                            finish()
                        }
                    )
                }
            }
        }
    }
}
