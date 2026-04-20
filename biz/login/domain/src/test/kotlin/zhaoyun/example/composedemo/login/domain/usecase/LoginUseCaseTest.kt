package zhaoyun.example.composedemo.login.domain.usecase

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
import zhaoyun.example.composedemo.login.domain.model.LoginEffect
import zhaoyun.example.composedemo.login.domain.model.LoginEvent
import zhaoyun.example.composedemo.service.usercenter.mock.FakeUserRepository

/**
 * LoginUseCase 单元测试 —— 验证登录成功与失败路径
 *
 * 通过 :service:user-center:mock 提供的 FakeUserRepository 模拟真实实现。
 * 纯构造函数注入，无需启动 Koin 容器。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginUseCaseTest {

    private val fakeRepository = FakeUserRepository()
    private lateinit var useCase: LoginUseCase

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        useCase = LoginUseCase(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun 正确的用户名密码登录成功并发射导航副作用() = runTest {
        val effects = mutableListOf<LoginEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase.effect.collect { effects.add(it) }
        }

        useCase.onEvent(LoginEvent.OnUsernameChanged("admin"))
        useCase.onEvent(LoginEvent.OnPasswordChanged("123456"))
        useCase.onEvent(LoginEvent.OnLoginClicked)

        assertFalse(useCase.state.value.isLoading)
        assertNull(useCase.state.value.errorMessage)
        assertEquals(listOf(LoginEffect.NavigateToHome), effects)
        assertTrue(fakeRepository.isLoggedIn())
        assertEquals("admin", fakeRepository.currentUser.value?.username)
    }

    @Test
    fun 错误的用户名密码登录失败并显示服务端错误() = runTest {
        val effects = mutableListOf<LoginEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            useCase.effect.collect { effects.add(it) }
        }

        useCase.onEvent(LoginEvent.OnUsernameChanged("wrong"))
        useCase.onEvent(LoginEvent.OnPasswordChanged("wrong"))
        useCase.onEvent(LoginEvent.OnLoginClicked)

        assertFalse(useCase.state.value.isLoading)
        assertEquals("用户名或密码错误", useCase.state.value.errorMessage)
        assertTrue(effects.isEmpty())
        assertFalse(fakeRepository.isLoggedIn())
        assertNull(fakeRepository.currentUser.value)
    }

    @Test
    fun 空输入时触发本地校验错误() = runTest {
        useCase.onEvent(LoginEvent.OnLoginClicked)

        assertEquals("用户名和密码不能为空", useCase.state.value.errorMessage)
        assertFalse(useCase.state.value.isLoading)
    }

    @Test
    fun 仅用户名为空时触发本地校验() = runTest {
        useCase.onEvent(LoginEvent.OnPasswordChanged("123456"))
        useCase.onEvent(LoginEvent.OnLoginClicked)

        assertEquals("用户名和密码不能为空", useCase.state.value.errorMessage)
        assertFalse(useCase.state.value.isLoading)
    }

    @Test
    fun 仅密码为空时触发本地校验() = runTest {
        useCase.onEvent(LoginEvent.OnUsernameChanged("admin"))
        useCase.onEvent(LoginEvent.OnLoginClicked)

        assertEquals("用户名和密码不能为空", useCase.state.value.errorMessage)
        assertFalse(useCase.state.value.isLoading)
    }
}
