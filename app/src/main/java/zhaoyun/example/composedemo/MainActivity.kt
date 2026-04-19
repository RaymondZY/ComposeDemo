package zhaoyun.example.composedemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import org.koin.android.ext.android.getKoin
import zhaoyun.example.composedemo.login.presentation.LoginActivity
import zhaoyun.example.composedemo.service.usercenter.api.UserRepository
import zhaoyun.example.composedemo.todo.presentation.TodoListActivity

/**
 * 应用唯一入口 —— 负责登录状态判断与路由分发
 *
 * 未登录 -> 启动 LoginActivity（通过 ActivityResult 等待回调）
 * 已登录 -> 跳转到 TodoListActivity
 */
class MainActivity : ComponentActivity() {

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // 登录成功，进入主页
            startActivity(Intent(this, TodoListActivity::class.java))
            finish()
        } else {
            // 用户取消或未登录，重新启动登录页
            launchLogin()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userRepository: UserRepository = getKoin().get()

        if (userRepository.isLoggedIn()) {
            startActivity(Intent(this, TodoListActivity::class.java))
            finish()
        } else {
            launchLogin()
        }
    }

    private fun launchLogin() {
        loginLauncher.launch(Intent(this, LoginActivity::class.java))
    }
}
