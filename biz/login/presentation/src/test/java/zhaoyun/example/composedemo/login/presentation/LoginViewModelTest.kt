package zhaoyun.example.composedemo.login.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import zhaoyun.example.composedemo.core.mvi.UiEffect
import zhaoyun.example.composedemo.login.domain.model.LoginEvent
import zhaoyun.example.composedemo.login.domain.usecase.LoginUseCase
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository

/**
 * LoginViewModel 单元测试 —— 覆盖状态机全部路径
 *
 * 通过 :service:user-center:mock 提供的 FakeUserRepository 模拟真实实现。
 * 纯构造函数注入，无需启动 Koin 容器。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val fakeRepository = FakeUserRepository()
    private fun createViewModel(): LoginViewModel = LoginViewModel(LoginUseCase(fakeRepository))

    @Test
    fun `初始状态为空且未加载`() {
        val viewModel = createViewModel()
        val state = viewModel.state.value

        assertEquals("", state.username)
        assertEquals("", state.password)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `输入用户名密码更新状态`() {
        val viewModel = createViewModel()

        viewModel.onEvent(LoginEvent.OnUsernameChanged("alice"))
        assertEquals("alice", viewModel.state.value.username)

        viewModel.onEvent(LoginEvent.OnPasswordChanged("secret"))
        assertEquals("secret", viewModel.state.value.password)
    }

    @Test
    fun `空输入时触发本地校验错误`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(LoginEvent.OnLoginClicked)

        assertEquals("用户名和密码不能为空", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `正确账号登录成功并发射导航副作用`() = runTest {
        val viewModel = createViewModel()
        val effects = mutableListOf<UiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effect.collect { effects.add(it) }
        }

        viewModel.onEvent(LoginEvent.OnUsernameChanged("admin"))
        viewModel.onEvent(LoginEvent.OnPasswordChanged("123456"))
        viewModel.onEvent(LoginEvent.OnLoginClicked)

        assertFalse(viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.errorMessage)
        assertEquals(listOf(UiEffect.NavigateToHome), effects)
    }

    @Test
    fun `错误账号登录失败并显示服务端错误`() = runTest {
        val viewModel = createViewModel()
        val effects = mutableListOf<UiEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effect.collect { effects.add(it) }
        }

        viewModel.onEvent(LoginEvent.OnUsernameChanged("bad"))
        viewModel.onEvent(LoginEvent.OnPasswordChanged("bad"))
        viewModel.onEvent(LoginEvent.OnLoginClicked)

        assertFalse(viewModel.state.value.isLoading)
        assertEquals("用户名或密码错误", viewModel.state.value.errorMessage)
        assertTrue(effects.isEmpty())
    }

    @Test
    fun `输入用户名时清除之前的错误消息`() {
        val viewModel = createViewModel()

        // 先触发错误
        viewModel.onEvent(LoginEvent.OnLoginClicked)
        assertEquals("用户名和密码不能为空", viewModel.state.value.errorMessage)

        // 输入用户名应清除错误
        viewModel.onEvent(LoginEvent.OnUsernameChanged("alice"))
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `输入密码时清除之前的错误消息`() {
        val viewModel = createViewModel()

        // 先触发错误
        viewModel.onEvent(LoginEvent.OnLoginClicked)
        assertEquals("用户名和密码不能为空", viewModel.state.value.errorMessage)

        // 输入密码应清除错误
        viewModel.onEvent(LoginEvent.OnPasswordChanged("secret"))
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun `仅用户名为空时触发本地校验`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(LoginEvent.OnPasswordChanged("123456"))
        viewModel.onEvent(LoginEvent.OnLoginClicked)

        assertEquals("用户名和密码不能为空", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `仅密码为空时触发本地校验`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(LoginEvent.OnUsernameChanged("admin"))
        viewModel.onEvent(LoginEvent.OnLoginClicked)

        assertEquals("用户名和密码不能为空", viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.isLoading)
    }
}
