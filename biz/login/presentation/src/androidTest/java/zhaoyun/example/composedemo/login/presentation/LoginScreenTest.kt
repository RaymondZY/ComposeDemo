package zhaoyun.example.composedemo.login.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import zhaoyun.example.composedemo.login.domain.usecase.LoginUseCase
import zhaoyun.example.composedemo.service.storage.mock.FakeKeyValueStorage
import zhaoyun.example.composedemo.service.usercenter.api.UserRepository
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository

/**
 * Login Compose UI 测试 —— 覆盖输入、校验、成功与失败路径
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val fakeRepository = FakeUserRepository()
    private val fakeStorage = FakeKeyValueStorage()

    @Before
    fun setup() {
        // 直接构造 ViewModel，无需 Koin
    }

    @After
    fun tearDown() {
        fakeRepository.logout()
        fakeStorage.clear()
    }

    private fun createViewModel(): LoginViewModel =
        LoginViewModel(LoginUseCase(fakeRepository, fakeStorage))

    @Test
    fun `初始状态显示输入框和登录按钮`() {
        composeTestRule.setContent {
            LoginScreen(onLoginSuccess = {}, viewModel = createViewModel())
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("用户登录").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_username_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_password_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("login_button").assertIsEnabled()
    }

    @Test
    fun `输入正确账号密码登录成功触发回调`() {
        var loginSuccessCalled = false

        composeTestRule.setContent {
            LoginScreen(
                onLoginSuccess = { loginSuccessCalled = true },
                viewModel = createViewModel()
            )
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("login_username_input").performTextInput("admin")
        composeTestRule.onNodeWithTag("login_password_input").performTextInput("123456")
        composeTestRule.onNodeWithTag("login_button").performClick()

        composeTestRule.waitForIdle()

        // 验证回调被调用
        assertTrue(loginSuccessCalled)
    }

    @Test
    fun `输入错误账号密码显示错误信息`() {
        composeTestRule.setContent {
            LoginScreen(onLoginSuccess = {}, viewModel = createViewModel())
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("login_username_input").performTextInput("bad")
        composeTestRule.onNodeWithTag("login_password_input").performTextInput("bad")
        composeTestRule.onNodeWithTag("login_button").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("login_error_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("用户名或密码错误").assertIsDisplayed()
    }

    @Test
    fun `空输入时显示本地校验错误`() {
        composeTestRule.setContent {
            LoginScreen(onLoginSuccess = {}, viewModel = createViewModel())
        }

        composeTestRule.waitForIdle()

        // 不输入任何内容直接点击登录
        composeTestRule.onNodeWithTag("login_button").performClick()

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("login_error_text").assertIsDisplayed()
        composeTestRule.onNodeWithText("用户名和密码不能为空").assertIsDisplayed()
    }
}
